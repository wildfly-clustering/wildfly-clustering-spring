/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.management.ObjectName;

import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalJmxConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.expiration.ExpirationManager;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.impl.ListenerInvocation;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.remoting.transport.jgroups.JGroupsChannelConfigurator;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.NonBlockingManager;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.jmx.JmxConfigurator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.server.group.GroupCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;
import org.wildfly.clustering.server.jgroups.dispatcher.JChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.jgroups.dispatcher.JChannelCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.spring.context.AutoDestroyBean;

/**
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerBean extends AutoDestroyBean implements ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration, InitializingBean, ResourceLoaderAware {

	private static final Logger LOGGER = Logger.getLogger(EmbeddedCacheManagerBean.class);
	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final InfinispanConfiguration configuration;

	private ResourceLoader loader;
	private EmbeddedCacheManager container;
	private JChannelCommandDispatcherFactory commandDispatcherFactory;

	public EmbeddedCacheManagerBean(InfinispanConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public GroupCommandDispatcherFactory<Address, ChannelGroupMember> getCommandDispatcherFactory() {
		return this.commandDispatcherFactory;
	}

	@Override
	public EmbeddedCacheManager getCacheContainer() {
		return this.container;
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String resourceName = this.configuration.getConfigurationResource();

		COUNTER.incrementAndGet();
		this.accept(() -> {
			// Stop RxJava schedulers when no longer in use
			if (COUNTER.decrementAndGet() == 0) {
				Schedulers.shutdown();
			}
		});

		Resource resource = this.loader.getResource(resourceName);
		if (resource == null) {
			throw new FileNotFoundException(resourceName);
		}
		ConfigurationBuilderHolder holder = new ParserRegistry().parse(resource.getInputStream(), MediaType.APPLICATION_XML);
		GlobalConfigurationBuilder global = holder.getGlobalConfigurationBuilder();
		String containerName = global.cacheContainer().name();
		TransportConfiguration transport = global.transport().create();

		JGroupsChannelConfigurator configurator = (transport.transport() != null) ? new JChannelConfigurator(transport, this.loader) : null;
		JChannel channel = (configurator != null) ? configurator.createChannel(null) : null;
		if (channel != null) {
			channel.setName(transport.nodeName());
			channel.setDiscardOwnMessages(true);
			channel.connect(transport.clusterName());
			this.accept(channel::close);

			GlobalJmxConfiguration jmx = global.jmx().create();
			if (jmx.enabled()) {
				ObjectName prefix = new ObjectName(jmx.domain(), "manager", ObjectName.quote(containerName));
				JmxConfigurator.registerChannel(channel, ManagementFactory.getPlatformMBeanServer(), prefix, transport.clusterName(), true);
				this.accept(() -> {
					try {
						JmxConfigurator.unregisterChannel(channel, ManagementFactory.getPlatformMBeanServer(), prefix, transport.clusterName());
					} catch (Exception e) {
						LOGGER.warn(e.getLocalizedMessage(), e);
					}
				});
			}

			Properties properties = new Properties();
			properties.put(JGroupsTransport.CHANNEL_CONFIGURATOR, new ForkChannelConfigurator(channel, containerName));
			global.transport().withProperties(properties);
		}

		this.commandDispatcherFactory = (channel != null) ? new JChannelCommandDispatcherFactory(new JChannelCommandDispatcherFactoryConfiguration() {
			@Override
			public JChannel getChannel() {
				return channel;
			}

			@Override
			public ByteBufferMarshaller getMarshaller() {
				return this.getMarshallerFactory().apply(JChannelCommandDispatcherFactory.class.getClassLoader());
			}

			@Override
			public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
				return loader -> new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(loader)).load(loader).build());
			}

			@Override
			public Predicate<Message> getUnknownForkPredicate() {
				return Predicate.not(Message::hasPayload);
			}
		}) : null;
		if (this.commandDispatcherFactory != null) {
			this.accept(this.commandDispatcherFactory::close);
		}

		ClassLoader loader = this.loader.getClassLoader();
		global.classLoader(this.loader.getClassLoader())
				.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER)
				.blockingThreadPool().threadFactory(new DefaultBlockingThreadFactory(BlockingManager.class))
				.expirationThreadPool().threadFactory(new DefaultBlockingThreadFactory(ExpirationManager.class))
				.listenerThreadPool().threadFactory(new DefaultBlockingThreadFactory(ListenerInvocation.class))
				.nonBlockingThreadPool().threadFactory(new DefaultNonBlockingThreadFactory(NonBlockingManager.class))
				.serialization()
					.marshaller(new UserMarshaller(MediaTypes.WILDFLY_PROTOSTREAM, new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(loader)).load(loader).build())))
					// Register dummy serialization context initializer, to bypass service loading in org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl
					// Otherwise marshaller auto-detection will not work
					.addContextInitializer(new SerializationContextInitializer() {
						@Deprecated
						@Override
						public String getProtoFile() {
							return null;
						}

						@Deprecated
						@Override
						public String getProtoFileName() {
							return null;
						}

						@Override
						public void registerMarshallers(SerializationContext context) {
						}

						@Override
						public void registerSchema(SerializationContext context) {
						}
					})
				.globalState().configurationStorage(ConfigurationStorage.IMMUTABLE).disable();

		this.container =new DefaultCacheManager(holder, false);
		this.container.start();
		this.accept(this.container::stop);
	}
}
