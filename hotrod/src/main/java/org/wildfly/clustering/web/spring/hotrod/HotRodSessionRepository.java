/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.spring.hotrod;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.impl.HotRodURI;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.CompositeIterable;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.immutable.CompositeImmutability;
import org.wildfly.clustering.ee.immutable.DefaultImmutability;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.manager.RemoteCacheManager;
import org.wildfly.clustering.infinispan.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderResolver;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionManagerFactory;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.hotrod.session.SessionManagerNearCacheFactory;
import org.wildfly.clustering.web.hotrod.sso.HotRodSSOManagerFactory;
import org.wildfly.clustering.web.hotrod.sso.HotRodSSOManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.clustering.web.spring.SpringSession;
import org.wildfly.clustering.web.spring.DistributableSessionRepository;
import org.wildfly.clustering.web.spring.DistributableSessionRepositoryConfiguration;
import org.wildfly.clustering.web.spring.ImmutableSessionDestroyAction;
import org.wildfly.clustering.web.spring.ImmutableSessionExpirationListener;
import org.wildfly.clustering.web.spring.IndexingConfiguration;
import org.wildfly.clustering.web.spring.SpringSpecificationProvider;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 */
public class HotRodSessionRepository implements FindByIndexNameSessionRepository<SpringSession>, InitializingBean, DisposableBean, LocalContextFactory<Void>, Registrar<String>, Registration {

    private final HotRodSessionRepositoryConfiguration configuration;
    private final List<Runnable> stopTasks = new LinkedList<>();
    private volatile FindByIndexNameSessionRepository<SpringSession> repository;

    public HotRodSessionRepository(HotRodSessionRepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        ServletContext context = this.configuration.getServletContext();
        // Deployment name = host name + context path + version
        String deploymentName = context.getVirtualServerName() + context.getContextPath();
        String templateName = this.configuration.getTemplateName();
        Integer maxActiveSessions = this.configuration.getMaxActiveSessions();
        SessionAttributePersistenceStrategy strategy = this.configuration.getPersistenceStrategy();

        ClassLoader containerLoader = WildFlySecurityManager.getClassLoaderPrivileged(HotRodSessionManagerFactory.class);
        URI uri = this.configuration.getUri();
        Configuration configuration = ((uri != null) ? HotRodURI.create(uri).toConfigurationBuilder() : new ConfigurationBuilder())
                .withProperties(this.configuration.getProperties())
                .marshaller(new ProtoStreamMarshaller(new ClassLoaderResolver(containerLoader), containerLoader))
                .classLoader(containerLoader)
                .build();

        configuration.addRemoteCache(deploymentName, builder -> builder.forceReturnValues(false).nearCacheMode(NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE).templateName(templateName));

        RemoteCacheContainer container = new RemoteCacheManager(this.getClass().getName(), configuration, this);
        container.start();
        this.stopTasks.add(container::stop);

        ByteBufferMarshaller marshaller = this.configuration.getMarshallerFactory().apply(context.getClassLoader());
        MarshalledValueFactory<ByteBufferMarshaller> marshalledValueFactory = new ByteBufferMarshalledValueFactory(marshaller);

        ServiceLoader<Immutability> loadedImmutability = ServiceLoader.load(Immutability.class, Immutability.class.getClassLoader());
        Immutability immutability = new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class), loadedImmutability));

        SessionManagerFactory<ServletContext, Void, TransactionBatch> managerFactory = new HotRodSessionManagerFactory<>(new HotRodSessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, ByteBufferMarshaller, Void>() {
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
            public MarshalledValueFactory<ByteBufferMarshaller> getMarshalledValueFactory() {
                return marshalledValueFactory;
            }

            @Override
            public String getServerName() {
                return context.getVirtualServerName();
            }

            @Override
            public LocalContextFactory<Void> getLocalContextFactory() {
                return HotRodSessionRepository.this;
            }

            @Override
            public <K, V> RemoteCache<K, V> getCache() {
                String cacheName = this.getDeploymentName();
                try (RemoteCacheContainer.NearCacheRegistration registration = container.registerNearCacheFactory(cacheName, new SessionManagerNearCacheFactory<>(this.getMaxActiveSessions(), this.getAttributePersistenceStrategy()))) {
                    return container.getCache(cacheName);
                }
            }

            @Override
            public Immutability getImmutability() {
                return immutability;
            }

            @Override
            public SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener> getSpecificationProvider() {
                return SpringSpecificationProvider.INSTANCE;
            }
        });
        this.stopTasks.add(managerFactory::close);

        Supplier<String> factory = this.configuration.getIdentifierFactory();
        IdentifierFactory<String> identifierFactory = new IdentifierFactory<String>() {
            @Override
            public String createIdentifier() {
                return factory.get();
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };

        Set<String> indexes = this.configuration.getIndexes();
        Map<String, SSOManager<Void, String, String, Void, TransactionBatch>> managers = indexes.isEmpty() ? Collections.emptyMap() : new HashMap<>();
        for (String indexName : indexes) {
            configuration.addRemoteCache(indexName, builder -> builder.forceReturnValues(false).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NONE).templateName(templateName));

            SSOManagerFactory<Void, String, String, TransactionBatch> ssoManagerFactory = new HotRodSSOManagerFactory<>(new HotRodSSOManagerFactoryConfiguration() {
                @Override
                public <K, V> RemoteCache<K, V> getRemoteCache() {
                    return container.getCache(indexName);
                }
            });

            SSOManager<Void, String, String, Void, TransactionBatch> ssoManager = ssoManagerFactory.createSSOManager(new SSOManagerConfiguration<ByteBufferMarshaller, Void>() {
                @Override
                public IdentifierFactory<String> getIdentifierFactory() {
                    return identifierFactory;
                }

                @Override
                public MarshalledValueFactory<ByteBufferMarshaller> getMarshalledValueFactory() {
                    return marshalledValueFactory;
                }

                @Override
                public LocalContextFactory<Void> getLocalContextFactory() {
                    return HotRodSessionRepository.this;
                }
            });
            managers.put(indexName, ssoManager);
        }
        IndexResolver<Session> resolver = this.configuration.getIndexResolver();
        IndexingConfiguration<TransactionBatch> indexing = new IndexingConfiguration<TransactionBatch>() {
            @Override
            public Map<String, SSOManager<Void, String, String, Void, TransactionBatch>> getSSOManagers() {
                return managers;
            }

            @Override
            public IndexResolver<Session> getIndexResolver() {
                return resolver;
            }
        };

        ApplicationEventPublisher publisher = this.configuration.getEventPublisher();
        BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> sessionDestroyAction = new ImmutableSessionDestroyAction<>(publisher, context, indexing);

        SessionExpirationListener expirationListener = new ImmutableSessionExpirationListener(context, sessionDestroyAction);

        SessionManager<Void, TransactionBatch> manager = managerFactory.createSessionManager(new SessionManagerConfiguration<ServletContext>() {
            @Override
            public ServletContext getServletContext() {
                return context;
            }

            @Override
            public IdentifierFactory<String> getIdentifierFactory() {
                return identifierFactory;
            }

            @Override
            public SessionExpirationListener getExpirationListener() {
                return expirationListener;
            }

            @Override
            public Recordable<ImmutableSession> getInactiveSessionRecorder() {
                // Spring session has no metrics capability
                return null;
            }
        });
        Optional<Duration> defaultTimeout = setDefaultMaxInactiveInterval(manager, Duration.ofMinutes(context.getSessionTimeout()));
        manager.start();
        this.stopTasks.add(manager::stop);

        this.repository = new DistributableSessionRepository<>(new DistributableSessionRepositoryConfiguration<TransactionBatch>() {
            @Override
            public SessionManager<Void, TransactionBatch> getSessionManager() {
                return manager;
            }

            @Override
            public Optional<Duration> getDefaultTimeout() {
                return defaultTimeout;
            }

            @Override
            public ApplicationEventPublisher getEventPublisher() {
                return publisher;
            }

            @Override
            public ServletContext getServletContext() {
                return context;
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

    private static Optional<Duration> setDefaultMaxInactiveInterval(SessionManager<Void, TransactionBatch> manager, Duration timeout) {
        try {
            manager.setDefaultMaxInactiveInterval(timeout);
            return Optional.empty();
        } catch (NoSuchMethodError error) {
            // Servlet version < 4.0
            return Optional.of(timeout);
        }
    }

    @Override
    public void destroy() throws Exception {
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
