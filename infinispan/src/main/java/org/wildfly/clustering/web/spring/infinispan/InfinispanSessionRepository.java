/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.spring.infinispan;

import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.management.ObjectName;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;

import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.Cache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalJmxConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.expiration.ExpirationManager;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.impl.ListenerInvocation;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsChannelConfigurator;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.NonBlockingManager;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.jmx.JmxConfigurator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.immutable.CompositeImmutability;
import org.wildfly.clustering.ee.immutable.DefaultImmutability;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.affinity.impl.DefaultKeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.marshall.InfinispanProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.NodeFactory;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.server.infinispan.dispatcher.LocalCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.group.CacheGroup;
import org.wildfly.clustering.server.infinispan.group.CacheGroupConfiguration;
import org.wildfly.clustering.server.infinispan.group.LocalGroup;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagerFactory;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaDataKey;
import org.wildfly.clustering.web.infinispan.sso.InfinispanSSOManagerFactory;
import org.wildfly.clustering.web.infinispan.sso.InfinispanSSOManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.clustering.web.spring.DistributableSessionRepository;
import org.wildfly.clustering.web.spring.DistributableSessionRepositoryConfiguration;
import org.wildfly.clustering.web.spring.ImmutableSessionDestroyAction;
import org.wildfly.clustering.web.spring.ImmutableSessionExpirationListener;
import org.wildfly.clustering.web.spring.IndexingConfiguration;
import org.wildfly.clustering.web.spring.JakartaSessionManagerConfiguration;
import org.wildfly.clustering.web.spring.SessionMarshallerFactory;
import org.wildfly.clustering.web.spring.SpringSession;
import org.wildfly.clustering.web.spring.SpringSpecificationProvider;
import org.wildfly.clustering.web.spring.security.SpringSecurityImmutability;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.common.iteration.CompositeIterable;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionRepository implements FindByIndexNameSessionRepository<SpringSession>, InitializingBean, DisposableBean, LocalContextFactory<Void>, Registrar<String>, Registration {

	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final InfinispanSessionRepositoryConfiguration configuration;
	private final List<Runnable> stopTasks = new LinkedList<>();
	private volatile FindByIndexNameSessionRepository<SpringSession> repository;

	public InfinispanSessionRepository(InfinispanSessionRepositoryConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ServletContext context = this.configuration.getServletContext();
		// Deployment name = host name + context path + version
		String deploymentName = context.getVirtualServerName() + context.getContextPath();
		String resourceName = this.configuration.getConfigurationResource();
		String templateName = this.configuration.getTemplateName();
		Integer maxActiveSessions = this.configuration.getMaxActiveSessions();
		SessionAttributePersistenceStrategy strategy = this.configuration.getPersistenceStrategy();

		COUNTER.incrementAndGet();
		this.stopTasks.add(() -> {
			// Stop RxJava schedulers when no longer in use
			if (COUNTER.decrementAndGet() == 0) {
				Schedulers.shutdown();
			}
		});

		ClassLoader containerLoader = WildFlySecurityManager.getClassLoaderPrivileged(InfinispanSessionManagerFactory.class);
		URL url = context.getResource(resourceName);
		if (url == null) {
			throw new FileNotFoundException(resourceName);
		}
		ConfigurationBuilderHolder holder = new ParserRegistry().parse(url);
		GlobalConfigurationBuilder global = holder.getGlobalConfigurationBuilder();
		String containerName = global.cacheContainer().name();
		TransportConfiguration transport = global.transport().create();

		JGroupsChannelConfigurator configurator = (transport.transport() != null) ? new JChannelConfigurator(context, transport) : null;
		JChannel channel = (configurator != null) ? configurator.createChannel(null) : null;
		if (channel != null) {
			channel.setName(transport.nodeName());
			channel.setDiscardOwnMessages(true);
			channel.connect(transport.clusterName());
			this.stopTasks.add(channel::close);

			GlobalJmxConfiguration jmx = global.jmx().create();
			if (jmx.enabled()) {
				ObjectName prefix = new ObjectName(jmx.domain(), "manager", ObjectName.quote(containerName));
				JmxConfigurator.registerChannel(channel, ManagementFactory.getPlatformMBeanServer(), prefix, transport.clusterName(), true);
				this.stopTasks.add(() -> {
					try {
						JmxConfigurator.unregisterChannel(channel, ManagementFactory.getPlatformMBeanServer(), prefix, transport.clusterName());
					} catch (Exception e) {
						context.log(e.getLocalizedMessage(), e);
					}
				});
			}

			Properties properties = new Properties();
			properties.put(JGroupsTransport.CHANNEL_CONFIGURATOR, new ForkChannelConfigurator(channel, containerName));
			global.transport().withProperties(properties);
		}

		Function<ClassLoader, ByteBufferMarshaller> marshallerFactory = this.configuration.getMarshallerFactory();

		ChannelCommandDispatcherFactory channelDispatcherFactory = (channel != null) ? new ChannelCommandDispatcherFactory(new ChannelCommandDispatcherFactoryConfiguration() {
			@Override
			public JChannel getChannel() {
				return channel;
			}

			@Override
			public ByteBufferMarshaller getMarshaller() {
				return SessionMarshallerFactory.PROTOSTREAM.apply(containerLoader);
			}

			@Override
			public Duration getTimeout() {
				return Duration.ofMillis(transport.initialClusterTimeout());
			}

			@Override
			public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
				return marshallerFactory;
			}

			@Override
			public Predicate<Message> getUnknownForkPredicate() {
				return Predicate.not(Message::hasPayload);
			}
		}) : null;
		Group<?> localGroup = new LocalGroup(transport.nodeName(), transport.clusterName());
		CommandDispatcherFactory dispatcherFactory = (channelDispatcherFactory != null) ? channelDispatcherFactory : new LocalCommandDispatcherFactory(localGroup);
		if (channelDispatcherFactory != null) {
			this.stopTasks.add(channelDispatcherFactory::close);
		}

		holder.getGlobalConfigurationBuilder()
				.classLoader(containerLoader)
				.blockingThreadPool().threadFactory(new DefaultThreadFactory(BlockingManager.class))
				.expirationThreadPool().threadFactory(new DefaultThreadFactory(ExpirationManager.class))
				.listenerThreadPool().threadFactory(new DefaultThreadFactory(ListenerInvocation.class))
				.nonBlockingThreadPool().threadFactory(new DefaultNonBlockingThreadFactory(NonBlockingManager.class))
				.serialization()
					.marshaller(new InfinispanProtoStreamMarshaller(new SimpleClassLoaderMarshaller(containerLoader), builder -> builder.load(containerLoader)))
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

		@SuppressWarnings("resource")
		EmbeddedCacheManager container =new DefaultCacheManager(holder, false);

		Configuration template = (templateName != null) ? container.getCacheConfiguration(templateName) : container.getDefaultCacheConfiguration();
		if (template == null) {
			if (templateName == null) {
				throw new IllegalArgumentException("Infinispan configuration does not define a default cache");
			}
			throw new IllegalArgumentException(String.format("No such configuration template: %s", templateName));
		}
		ConfigurationBuilder builder = new ConfigurationBuilder().read(template).template(false);
		builder.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
		builder.clustering().hash().groups().enabled();

		// Disable expiration, if necessary
		ExpirationConfiguration expiration = builder.expiration().create();
		if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
			builder.expiration().lifespan(-1).maxIdle(-1);
		}

		EvictionStrategy eviction = (maxActiveSessions != null) ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
		builder.memory().storage(StorageType.HEAP)
				.whenFull(eviction)
				.maxCount((maxActiveSessions != null) ? maxActiveSessions.longValue() : -1)
				;
		if (eviction.isEnabled()) {
			// Only evict creation meta-data entries
			// We will cascade eviction to the remaining entries for a given session
			builder.addModule(DataContainerConfigurationBuilder.class).evictable(SessionCreationMetaDataKey.class::isInstance);
		}

		container.defineConfiguration(deploymentName, builder.build());
		this.stopTasks.add(() -> container.undefineConfiguration(deploymentName));

		container.start();
		this.stopTasks.add(container::stop);

		ClassLoader loader = context.getClassLoader();
		ByteBufferMarshaller marshaller = marshallerFactory.apply(loader);

		List<Immutability> loadedImmutabilities = new LinkedList<>();
		for (Immutability loadedImmutability : ServiceLoader.load(Immutability.class, loader)) {
			loadedImmutabilities.add(loadedImmutability);
		}
		Immutability immutability = new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class), EnumSet.allOf(SpringSecurityImmutability.class), loadedImmutabilities));

		Supplier<String> identifierFactory = this.configuration.getIdentifierFactory();

		KeyAffinityServiceFactory affinityFactory = new DefaultKeyAffinityServiceFactory();

		Map<String, String> indexes = this.configuration.getIndexes();
		Map<String, SSOManager<Void, String, String, Void, TransactionBatch>> managers = indexes.isEmpty() ? Collections.emptyMap() : new HashMap<>();
		for (Map.Entry<String, String> entry : indexes.entrySet()) {
			String cacheName = String.format("%s/%s", deploymentName, entry.getKey());
			String indexName = entry.getValue();

			container.defineConfiguration(cacheName, builder.build());
			this.stopTasks.add(() -> container.undefineConfiguration(cacheName));

			Cache<?, ?> cache = container.getCache(cacheName);
			cache.start();
			this.stopTasks.add(cache::stop);

			SSOManagerFactory<Void, String, String, TransactionBatch> ssoManagerFactory = new InfinispanSSOManagerFactory<>(new InfinispanSSOManagerFactoryConfiguration() {
				@Override
				public <K, V> Cache<K, V> getCache() {
					return container.getCache(cacheName);
				}

				@Override
				public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
					return affinityFactory;
				}
			});

			SSOManager<Void, String, String, Void, TransactionBatch> ssoManager = ssoManagerFactory.createSSOManager(new SSOManagerConfiguration<>() {
				@Override
				public Supplier<String> getIdentifierFactory() {
					return identifierFactory;
				}

				@Override
				public ByteBufferMarshaller getMarshaller() {
					return marshaller;
				}

				@Override
				public LocalContextFactory<Void> getLocalContextFactory() {
					return InfinispanSessionRepository.this;
				}
			});
			managers.put(indexName, ssoManager);
		}
		IndexResolver<Session> resolver = this.configuration.getIndexResolver();
		IndexingConfiguration<TransactionBatch> indexing = new IndexingConfiguration<>() {
			@Override
			public Map<String, SSOManager<Void, String, String, Void, TransactionBatch>> getSSOManagers() {
				return managers;
			}

			@Override
			public IndexResolver<Session> getIndexResolver() {
				return resolver;
			}
		};

		Cache<?, ?> cache = container.getCache(deploymentName);
		cache.start();
		this.stopTasks.add(cache::stop);

		NodeFactory<org.jgroups.Address> memberFactory = (channelDispatcherFactory != null) ? channelDispatcherFactory : address -> localGroup.createNode(null);
		CacheGroup group = new CacheGroup(new CacheGroupConfiguration() {
			@SuppressWarnings("unchecked")
			@Override
			public <K, V> Cache<K, V> getCache() {
				return (Cache<K, V>) cache;
			}

			@Override
			public NodeFactory<org.jgroups.Address> getMemberFactory() {
				return memberFactory;
			}
		});
		this.stopTasks.add(group::close);

		SessionManagerFactory<ServletContext, Void, TransactionBatch> managerFactory = new InfinispanSessionManagerFactory<>(new InfinispanSessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, Void>() {
			@Override
			public Integer getMaxActiveSessions() {
				return maxActiveSessions;
			}

			@Override
			public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
				return strategy;
			}

			@Override
			public String getDeploymentName() {
				return deploymentName;
			}

			@Override
			public ByteBufferMarshaller getMarshaller() {
				return marshaller;
			}

			@Override
			public String getServerName() {
				return context.getVirtualServerName();
			}

			@Override
			public LocalContextFactory<Void> getLocalContextFactory() {
				return InfinispanSessionRepository.this;
			}

			@Override
			public <K, V> Cache<K, V> getCache() {
				return container.getCache(this.getDeploymentName());
			}

			@Override
			public Immutability getImmutability() {
				return immutability;
			}

			@Override
			public SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener> getSpecificationProvider() {
				return SpringSpecificationProvider.INSTANCE;
			}

			@Override
			public CommandDispatcherFactory getCommandDispatcherFactory() {
				return dispatcherFactory;
			}

			@Override
			public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
				return affinityFactory;
			}

			@Override
			public NodeFactory<Address> getMemberFactory() {
				return group;
			}
		});
		this.stopTasks.add(managerFactory::close);

		ApplicationEventPublisher publisher = this.configuration.getEventPublisher();
		BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> sessionDestroyAction = new ImmutableSessionDestroyAction<>(publisher, context, indexing);

		Consumer<ImmutableSession> expirationListener = new ImmutableSessionExpirationListener(context, sessionDestroyAction);

		SessionManagerConfiguration<ServletContext> managerConfiguration = new JakartaSessionManagerConfiguration() {
			@Override
			public ServletContext getServletContext() {
				return context;
			}

			@Override
			public Supplier<String> getIdentifierFactory() {
				return identifierFactory;
			}

			@Override
			public Consumer<ImmutableSession> getExpirationListener() {
				return expirationListener;
			}
		};
		SessionManager<Void, TransactionBatch> manager = managerFactory.createSessionManager(managerConfiguration);
		manager.start();
		this.stopTasks.add(manager::stop);

		this.repository = new DistributableSessionRepository<>(new DistributableSessionRepositoryConfiguration<TransactionBatch>() {
			@Override
			public SessionManager<Void, TransactionBatch> getSessionManager() {
				return manager;
			}

			@Override
			public ApplicationEventPublisher getEventPublisher() {
				return publisher;
			}

			@Override
			public BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> getSessionDestroyAction() {
				return sessionDestroyAction;
			}

			@Override
			public IndexingConfiguration<TransactionBatch> getIndexingConfiguration() {
				return indexing;
			}
		});
	}

	@Override
	public void destroy() {
		// Stop in reverse order
		ListIterator<Runnable> tasks = this.stopTasks.listIterator(this.stopTasks.size() - 1);
		while (tasks.hasPrevious()) {
			tasks.previous().run();
		}
	}

	@Override
	public Registration register(String name) {
		return this;
	}

	@Override
	public void close() {
	}

	@Override
	public Void createLocalContext() {
		return null;
	}

	@Override
	public SpringSession createSession() {
		return this.repository.createSession();
	}

	@Override
	public SpringSession findById(String id) {
		return this.repository.findById(id);
	}

	@Override
	public void deleteById(String id) {
		this.repository.deleteById(id);
	}

	@Override
	public void save(SpringSession session) {
		this.repository.save(session);
	}

	@Override
	public Map<String, SpringSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
		return this.repository.findByIndexNameAndIndexValue(indexName, indexValue);
	}
}
