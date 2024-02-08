/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.impl.HotRodURI;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.executors.NonBlockingResource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.cache.infinispan.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.container.ContainerFacadeProvider;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionManagerFactory;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.remote.user.HotRodUserManagerFactory;
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
import org.wildfly.common.function.Functions;

/**
 * A session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodSessionRepository implements InitializingBean, DisposableBean, Consumer<DistributableSessionRepositoryConfiguration<TransactionBatch>> {

	static class NonBlockingThreadGroup extends ThreadGroup implements NonBlockingResource {
		NonBlockingThreadGroup(String name) {
			super(name);
		}
	 }

	private final HotRodSessionRepositoryConfiguration configuration;
	private final List<Runnable> stopTasks = new LinkedList<>();

	public AbstractHotRodSessionRepository(HotRodSessionRepositoryConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		ServletContext context = this.configuration.getServletContext();
		// Deployment name = host name + context path + version
		String deploymentName = context.getVirtualServerName() + context.getContextPath();
		String templateName = this.configuration.getTemplateName();
		OptionalInt maxActiveSessions = this.configuration.getMaxActiveSessions();
		SessionAttributePersistenceStrategy strategy = this.configuration.getPersistenceStrategy();

		ClassLoader containerLoader = HotRodSessionManagerFactory.class.getClassLoader();
		URI uri = this.configuration.getUri();
		Configuration configuration = ((uri != null) ? HotRodURI.create(uri).toConfigurationBuilder() : new ConfigurationBuilder())
				.withProperties(this.configuration.getProperties())
				.marshaller(new ProtoStreamMarshaller(ClassLoaderMarshaller.of(containerLoader), builder -> builder.load(containerLoader)))
				.classLoader(containerLoader)
				.asyncExecutorFactory().factory(new ExecutorFactory() {
					@Override
					public ThreadPoolExecutor getExecutor(Properties p) {
						ConfigurationProperties properties = new ConfigurationProperties(p);
						String threadNamePrefix = properties.getDefaultExecutorFactoryThreadNamePrefix();
						String threadNameSuffix = properties.getDefaultExecutorFactoryThreadNameSuffix();
						NonBlockingThreadGroup group = new NonBlockingThreadGroup(threadNamePrefix + "-group");
						ThreadFactory factory = new ThreadFactory() {
							private final AtomicInteger counter = new AtomicInteger(0);

							@Override
							public Thread newThread(Runnable task) {
								int threadIndex = this.counter.incrementAndGet();
								Thread thread = new Thread(group, task, threadNamePrefix + "-" + threadIndex + threadNameSuffix);
								thread.setDaemon(true);
								return thread;
							}
						};

						return new ThreadPoolExecutor(properties.getDefaultExecutorFactoryPoolSize(), properties.getDefaultExecutorFactoryPoolSize(), 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), new DefaultThreadFactory(factory));
					}
				})
				.build();

		configuration.addRemoteCache(deploymentName, builder -> builder.forceReturnValues(false).nearCacheMode(maxActiveSessions.isEmpty() ? NearCacheMode.DISABLED : NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE).templateName(templateName));

		@SuppressWarnings("resource")
		RemoteCacheContainer container = new RemoteCacheManager(configuration);
		container.start();
		this.stopTasks.add(container::stop);

		ClassLoader loader = context.getClassLoader();
		ByteBufferMarshaller marshaller = this.configuration.getMarshallerFactory().apply(loader);

		List<Immutability> loadedImmutabilities = new LinkedList<>();
		for (Immutability loadedImmutability : ServiceLoader.load(Immutability.class, loader)) {
			loadedImmutabilities.add(loadedImmutability);
		}
		Immutability immutability = Immutability.composite(Stream.concat(Stream.of(Immutability.getDefault(), SpringSecurityImmutability.INSTANCE), loadedImmutabilities.stream()).toList());

		int expirationThreadPoolSize = this.configuration.getExpirationThreadPoolSize();

		HotRodSessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, Void> sessionManagerFactoryConfiguration = new HotRodSessionManagerFactoryConfiguration<>() {
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
			public <K, V> RemoteCache<K, V> getCache() {
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
			public int getExpirationThreadPoolSize() {
				return expirationThreadPoolSize;
			}
		};
		SessionManagerFactory<ServletContext, Void, TransactionBatch> managerFactory = new HotRodSessionManagerFactory<>(sessionManagerFactoryConfiguration);
		this.stopTasks.add(managerFactory::close);

		Supplier<String> identifierFactory = this.configuration.getIdentifierFactory();

		Map<String, String> indexes = this.configuration.getIndexes();
		Map<String, UserManager<Void, Void, String, String, TransactionBatch>> managers = indexes.isEmpty() ? Map.of() : new HashMap<>();
		for (Map.Entry<String, String> entry : indexes.entrySet()) {
			String cacheName = String.format("%s/%s", deploymentName, entry.getKey());
			String indexName = entry.getValue();
			configuration.addRemoteCache(cacheName, builder -> builder.forceReturnValues(false).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NONE).templateName(templateName));

			RemoteCacheConfiguration cacheConfiguration = new RemoteCacheConfiguration() {
				@Override
				public <CK, CV> RemoteCache<CK, CV> getCache() {
					return container.getCache(cacheName);
				}
			};

			UserManagerFactory<Void, String, String, TransactionBatch> userManagerFactory = new HotRodUserManagerFactory<>(cacheConfiguration);

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
		SessionManager<Void, TransactionBatch> manager = managerFactory.createSessionManager(managerConfiguration);
		manager.start();
		this.stopTasks.add(manager::stop);

		this.accept(new DistributableSessionRepositoryConfiguration<TransactionBatch>() {
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
	public void destroy() throws Exception {
		// Stop in reverse order
		ListIterator<Runnable> tasks = this.stopTasks.listIterator(this.stopTasks.size() - 1);
		while (tasks.hasPrevious()) {
			tasks.previous().run();
		}
	}
}
