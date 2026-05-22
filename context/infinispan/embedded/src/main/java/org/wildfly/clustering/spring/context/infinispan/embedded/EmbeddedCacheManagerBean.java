/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.management.ObjectName;

import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.executors.ScheduledThreadPoolExecutorFactory;
import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalJmxConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.expiration.ExpirationManager;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.threads.CoreExecutorFactory;
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
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.jmx.JmxConfigurator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
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
import org.wildfly.clustering.spring.context.AutoDestroyBean;

/**
 * A Spring bean that configures and provides an embedded cache manager.
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerBean extends AutoDestroyBean implements ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration, InitializingBean, ResourceLoaderAware, EnvironmentAware {

	private static final System.Logger LOGGER = System.getLogger(EmbeddedCacheManagerBean.class.getPackageName());
	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final InfinispanConfiguration configuration;

	private ResourceLoader loader;
	private PropertyResolver resolver;
	private EmbeddedCacheManager container;
	private JChannelCommandDispatcherFactory commandDispatcherFactory;

	/**
	 * Creates an embedded cache manager bean from the specified configuration.
	 * @param configuration Infinispan configuration.
	 */
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
	public void setEnvironment(Environment environment) {
		this.resolver = environment;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String resourceName = this.configuration.getResource();

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
		String xml = this.resolver.resolvePlaceholders(resource.getContentAsString(StandardCharsets.UTF_8));
		ConfigurationBuilderHolder holder = new ParserRegistry(this.loader.getClassLoader(), false, System.getProperties()).parse(xml, MediaType.APPLICATION_XML);
		GlobalConfigurationBuilder global = holder.getGlobalConfigurationBuilder();
		String containerName = global.cacheContainer().name();
		TransportConfiguration transport = global.transport().create();

		JGroupsChannelConfigurator configurator = (transport.transport() != null) ? new JChannelConfigurator(transport, this.loader) : null;
		JChannel channel = (configurator != null) ? configurator.createChannel(null) : null;
		if (channel != null) {
			channel.setName(transport.nodeName());
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
						LOGGER.log(System.Logger.Level.WARNING, e.getLocalizedMessage(), e);
					}
				});
			}

			Properties properties = new Properties();
			properties.put(JGroupsTransport.CHANNEL_CONFIGURATOR, new ForkChannelConfigurator(channel, containerName));
			global.transport().withProperties(properties);
		}

		this.commandDispatcherFactory = (channel != null) ? new JChannelCommandDispatcherFactory(new JChannelCommandDispatcherFactory.Configuration() {
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

		Map<String, ExecutorServiceFactory<? extends ExecutorService>> executors = Map.of(
				KnownComponentNames.BLOCKING_EXECUTOR, new SimpleThreadPoolExecutorFactory<>(createThreadPoolFactoryWithDefaults(KnownComponentNames.BLOCKING_EXECUTOR)),
				KnownComponentNames.EXPIRATION_SCHEDULED_EXECUTOR, new SimpleThreadPoolExecutorFactory<>(ScheduledThreadPoolExecutorFactory.create()),
				KnownComponentNames.ASYNC_NOTIFICATION_EXECUTOR, new SimpleThreadPoolExecutorFactory<>(createThreadPoolFactoryWithDefaults(KnownComponentNames.ASYNC_NOTIFICATION_EXECUTOR)),
				KnownComponentNames.NON_BLOCKING_EXECUTOR, new SimpleNonBlockingThreadPoolExecutorFactory<>(createThreadPoolFactoryWithDefaults(KnownComponentNames.NON_BLOCKING_EXECUTOR)));

		executors.values().forEach(this);

		ClassLoader loader = this.loader.getClassLoader();
		global.classLoader(this.loader.getClassLoader())
				.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER)
				.blockingThreadPool().threadPoolFactory(executors.get(KnownComponentNames.BLOCKING_EXECUTOR)).threadFactory(new DefaultBlockingThreadFactory(BlockingManager.class))
				.expirationThreadPool().threadPoolFactory(executors.get(KnownComponentNames.EXPIRATION_SCHEDULED_EXECUTOR)).threadFactory(new DefaultBlockingThreadFactory(ExpirationManager.class))
				.listenerThreadPool().threadPoolFactory(executors.get(KnownComponentNames.ASYNC_NOTIFICATION_EXECUTOR)).threadFactory(new DefaultBlockingThreadFactory(ListenerInvocation.class))
				.nonBlockingThreadPool().threadPoolFactory(executors.get(KnownComponentNames.NON_BLOCKING_EXECUTOR)).threadFactory(new DefaultNonBlockingThreadFactory(NonBlockingManager.class))
				.serialization()
					.marshaller(new UserMarshaller(MediaTypes.WILDFLY_PROTOSTREAM, new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(loader)).load(loader).build())))
					// Register dummy serialization context initializer, to bypass service loading in org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl
					// Otherwise marshaller auto-detection will not work
					.addContextInitializer(new SerializationContextInitializer() {
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

	private interface ExecutorServiceFactory<E extends ExecutorService> extends ThreadPoolExecutorFactory<E>, Runnable {
	}

	private static ThreadPoolExecutorFactory<? extends ExecutorService> createThreadPoolFactoryWithDefaults(String componentName) {
		int defaultQueueSize = KnownComponentNames.getDefaultQueueSize(componentName);
		int defaultMaxThreads = KnownComponentNames.getDefaultThreads(componentName);
		return CoreExecutorFactory.executorFactory(defaultMaxThreads, defaultQueueSize, KnownComponentNames.NON_BLOCKING_EXECUTOR.equals(componentName));
	}

	private static class SimpleThreadPoolExecutorFactory<E extends ExecutorService> implements ExecutorServiceFactory<E> {
		private final List<Runnable> tasks = new CopyOnWriteArrayList<>();
		private final ThreadPoolExecutorFactory<E> factory;

		SimpleThreadPoolExecutorFactory(ThreadPoolExecutorFactory<E> factory) {
			this.factory = factory;
		}

		@Override
		public E createExecutor(ThreadFactory factory) {
			E executor = this.factory.createExecutor(factory);
			this.tasks.add(new Runnable() {
				private final Duration timeout = TransactionConfiguration.CACHE_STOP_TIMEOUT.getDefaultValue().toDuration();

				@Override
				public void run() {
					try {
						executor.awaitTermination(this.timeout.toNanos(), TimeUnit.NANOSECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			});
			return executor;
		}

		@Override
		public void validate() {
			// Do nothing
		}

		@Override
		public void run() {
			this.tasks.forEach(Runnable::run);
		}
	}

	private static class SimpleNonBlockingThreadPoolExecutorFactory<E extends ExecutorService> extends SimpleThreadPoolExecutorFactory<E> {

		SimpleNonBlockingThreadPoolExecutorFactory(ThreadPoolExecutorFactory<E> factory) {
			super(factory);
		}

		@Override
		public boolean createsNonBlockingThreads() {
			return true;
		}
	}
}
