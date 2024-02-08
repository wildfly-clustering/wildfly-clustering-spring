/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.cache.infinispan.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.group.GroupCommandDispatcherFactory;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.server.infinispan.dispatcher.EmbeddedCacheManagerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.LocalEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;
import org.wildfly.clustering.server.jgroups.dispatcher.JChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.jgroups.dispatcher.JChannelCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.container.ContainerFacadeProvider;
import org.wildfly.clustering.session.infinispan.embedded.InfinispanSessionManagerFactory;
import org.wildfly.clustering.session.infinispan.embedded.InfinispanSessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.embedded.metadata.SessionMetaDataKey;
import org.wildfly.clustering.session.infinispan.embedded.user.InfinispanUserManagerFactory;
import org.wildfly.clustering.session.user.UserManager;
import org.wildfly.clustering.session.user.UserManagerConfiguration;
import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.clustering.spring.security.SpringSecurityImmutability;
import org.wildfly.clustering.spring.session.DistributableSessionRepositoryConfiguration;
import org.wildfly.clustering.spring.session.ImmutableSessionDestroyAction;
import org.wildfly.clustering.spring.session.ImmutableSessionExpirationListener;
import org.wildfly.clustering.spring.session.IndexingConfiguration;
import org.wildfly.clustering.spring.session.JakartaServletFacadeProvider;
import org.wildfly.clustering.spring.session.JakartaSessionManagerConfiguration;
import org.wildfly.clustering.spring.session.SessionMarshallerFactory;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionRepository implements InitializingBean, DisposableBean, Consumer<DistributableSessionRepositoryConfiguration<TransactionBatch>>, Registrar<String>, Registration {

	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final InfinispanSessionRepositoryConfiguration configuration;
	private final List<Runnable> stopTasks = new LinkedList<>();

	public AbstractInfinispanSessionRepository(InfinispanSessionRepositoryConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ServletContext context = this.configuration.getServletContext();
		// Deployment name = host name + context path + version
		String deploymentName = context.getVirtualServerName() + context.getContextPath();
		String resourceName = this.configuration.getConfigurationResource();
		String templateName = this.configuration.getTemplateName();
		OptionalInt maxActiveSessions = this.configuration.getMaxActiveSessions();
		SessionAttributePersistenceStrategy strategy = this.configuration.getPersistenceStrategy();

		COUNTER.incrementAndGet();
		this.stopTasks.add(() -> {
			// Stop RxJava schedulers when no longer in use
			if (COUNTER.decrementAndGet() == 0) {
				Schedulers.shutdown();
			}
		});

		ClassLoader containerLoader = InfinispanSessionManagerFactory.class.getClassLoader();
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

		JChannelCommandDispatcherFactory channelDispatcherFactory = (channel != null) ? new JChannelCommandDispatcherFactory(new JChannelCommandDispatcherFactoryConfiguration() {
			@Override
			public JChannel getChannel() {
				return channel;
			}

			@Override
			public ByteBufferMarshaller getMarshaller() {
				return this.getMarshallerFactory().apply(containerLoader);
			}

			@Override
			public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
				return SessionMarshallerFactory.PROTOSTREAM;
			}

			@Override
			public Predicate<Message> getUnknownForkPredicate() {
				return Predicate.not(Message::hasPayload);
			}
		}) : null;
		if (channelDispatcherFactory != null) {
			this.stopTasks.add(channelDispatcherFactory::close);
		}

		global.classLoader(containerLoader)
				.blockingThreadPool().threadFactory(new DefaultThreadFactory(BlockingManager.class))
				.expirationThreadPool().threadFactory(new DefaultThreadFactory(ExpirationManager.class))
				.listenerThreadPool().threadFactory(new DefaultThreadFactory(ListenerInvocation.class))
				.nonBlockingThreadPool().threadFactory(new DefaultNonBlockingThreadFactory(NonBlockingManager.class))
				.serialization()
					.marshaller(new ProtoStreamMarshaller(ClassLoaderMarshaller.of(containerLoader), builder -> builder.load(containerLoader)))
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

		EvictionStrategy eviction = maxActiveSessions.isPresent() ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
		builder.memory().storage(StorageType.HEAP)
				.whenFull(eviction)
				.maxCount(maxActiveSessions.orElse(-1))
				;
		if (eviction.isEnabled()) {
			// Only evict meta-data entries
			// We will cascade eviction to the remaining entries for a given session
			builder.addModule(DataContainerConfigurationBuilder.class).evictable(SessionMetaDataKey.class::isInstance);
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
		Immutability immutability = Immutability.composite(Stream.concat(Stream.of(Immutability.getDefault(), SpringSecurityImmutability.INSTANCE), loadedImmutabilities.stream()).toList());

		Supplier<String> identifierFactory = this.configuration.getIdentifierFactory();

		Map<String, String> indexes = this.configuration.getIndexes();
		Map<String, UserManager<Void, Void, String, String, TransactionBatch>> managers = indexes.isEmpty() ? Map.of() : new HashMap<>();
		for (Map.Entry<String, String> entry : indexes.entrySet()) {
			String cacheName = String.format("%s/%s", deploymentName, entry.getKey());
			String indexName = entry.getValue();

			container.defineConfiguration(cacheName, builder.build());
			this.stopTasks.add(() -> container.undefineConfiguration(cacheName));

			Cache<?, ?> cache = container.getCache(cacheName);
			cache.start();
			this.stopTasks.add(cache::stop);

			EmbeddedCacheConfiguration cacheConfiguration = new EmbeddedCacheConfiguration() {
				@Override
				public <K, V> Cache<K, V> getCache() {
					return container.getCache(cacheName);
				}
			};
			UserManagerFactory<Void, String, String, TransactionBatch> userManagerFactory = new InfinispanUserManagerFactory<>(cacheConfiguration);

			UserManager<Void, Void, String, String, TransactionBatch> userManager = userManagerFactory.createUserManager(new UserManagerConfiguration<>() {
				@Override
				public Supplier<String> getIdentifierFactory() {
					return identifierFactory;
				}

				@Override
				public ByteBufferMarshaller getMarshaller() {
					return marshaller;
				}

				@Override
				public Supplier<Void> getTransientContextFactory() {
					return Functions.constantSupplier(null);
				}
			});
			managers.put(indexName, userManager);
		}
		IndexResolver<Session> resolver = this.configuration.getIndexResolver();
		IndexingConfiguration<TransactionBatch> indexing = new IndexingConfiguration<>() {
			@Override
			public Map<String, UserManager<Void, Void, String, String, TransactionBatch>> getUserManagers() {
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

		CacheContainerCommandDispatcherFactory dispatcherFactory = (channelDispatcherFactory != null) ? new EmbeddedCacheManagerCommandDispatcherFactory<>(new ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration() {
			@Override
			public GroupCommandDispatcherFactory<org.jgroups.Address, ChannelGroupMember> getCommandDispatcherFactory() {
				return channelDispatcherFactory;
			}
			
			@Override
			public EmbeddedCacheManager getCacheContainer() {
				return container;
			}
		}) : new EmbeddedCacheManagerCommandDispatcherFactory<>(new LocalEmbeddedCacheManagerCommandDispatcherFactoryConfiguration() {
			@Override
			public EmbeddedCacheManager getCacheContainer() {
				return container;
			}
		});

		InfinispanSessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, Void, CacheContainerGroupMember> sessionManagerFactoryConfiguration = new InfinispanSessionManagerFactoryConfiguration<>() {
			@Override
			public OptionalInt getMaxActiveSessions() {
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
			public Supplier<Void> getSessionContextFactory() {
				return Functions.constantSupplier(null);
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
			public ContainerFacadeProvider<HttpSession, ServletContext, HttpSessionActivationListener> getContainerFacadeProvider() {
				return JakartaServletFacadeProvider.INSTANCE;
			}

			@Override
			public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
				return dispatcherFactory;
			}
		};
		SessionManagerFactory<ServletContext, Void, TransactionBatch> sessionManagerFactory = new InfinispanSessionManagerFactory<>(sessionManagerFactoryConfiguration);
		this.stopTasks.add(sessionManagerFactory::close);

		ApplicationEventPublisher publisher = this.configuration.getEventPublisher();
		BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> sessionDestroyAction = new ImmutableSessionDestroyAction<>(publisher, context, indexing);

		Consumer<ImmutableSession> expirationListener = new ImmutableSessionExpirationListener(context, sessionDestroyAction);

		SessionManagerConfiguration<ServletContext> managerConfiguration = new JakartaSessionManagerConfiguration<>() {
			@Override
			public ServletContext getContext() {
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
		SessionManager<Void, TransactionBatch> sessionManager = sessionManagerFactory.createSessionManager(managerConfiguration);
		sessionManager.start();
		this.stopTasks.add(sessionManager::stop);

		this.accept(new DistributableSessionRepositoryConfiguration<TransactionBatch>() {
			@Override
			public SessionManager<Void, TransactionBatch> getSessionManager() {
				return sessionManager;
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
}
