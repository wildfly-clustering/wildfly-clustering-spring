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

import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.jboss.marshalling.MarshallingConfiguration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.SessionRepository;
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
import org.wildfly.clustering.infinispan.marshalling.jboss.JBossMarshaller;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionManagerFactory;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.hotrod.session.SessionManagerNearCacheFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.clustering.web.spring.DistributableSession;
import org.wildfly.clustering.web.spring.DistributableSessionRepository;
import org.wildfly.clustering.web.spring.ImmutableSessionExpirationListener;
import org.wildfly.clustering.web.spring.SpringSpecificationProvider;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionRepository implements SessionRepository<DistributableSession<TransactionBatch>>, InitializingBean, DisposableBean, LocalContextFactory<Void>, Registrar<String>, Registration {

    enum JBossMarshallingVersion implements Function<ClassLoader, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(ClassLoader loader) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassTable(new DynamicClassTable(loader));
                config.setObjectTable(new ExternalizerObjectTable(loader));
                return config;
            }
        },
        ;
        static final JBossMarshallingVersion CURRENT = VERSION_1;
    }

    private final HotRodSessionRepositoryConfiguration configuration;
    private volatile RemoteCacheContainer container;
    private volatile SessionManagerFactory<ServletContext, Void, TransactionBatch> managerFactory;
    private volatile SessionManager<Void, TransactionBatch> manager;
    private volatile SessionRepository<DistributableSession<TransactionBatch>> repository;

    public HotRodSessionRepository(HotRodSessionRepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Integer maxActiveSessions = this.configuration.getMaxActiveSessions();
        ClassLoader managerLoader = HotRodSessionManagerFactory.class.getClassLoader();
        @SuppressWarnings("deprecation")
        Configuration configuration = new ConfigurationBuilder()
                .withProperties(this.configuration.getProperties())
                .marshaller(new JBossMarshaller(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, managerLoader), managerLoader))
                .nearCache().mode(NearCacheMode.INVALIDATED).maxEntries(-1)
                .build();

        SessionAttributePersistenceStrategy strategy = this.configuration.getPersistenceStrategy();
        String configurationName = this.configuration.getConfigurationName();

        ServletContext context = this.configuration.getServletContext();
        ClassLoader deploymentLoader = this.configuration.getClassLoader();
        String containerName = context.getServletContextName();
        RemoteCacheContainer container = new RemoteCacheManager(containerName, configuration, this);
        container.start();
        this.container = container;

        MarshallingContext marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, deploymentLoader), deploymentLoader);
        MarshalledValueFactory<MarshallingContext> marshalledValueFactory = new SimpleMarshalledValueFactory(marshallingContext);

        ServiceLoader<Immutability> loadedImmutability = ServiceLoader.load(Immutability.class, Immutability.class.getClassLoader());
        Immutability immutability = new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class), loadedImmutability));

        HotRodSessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, HttpSessionBindingListener, MarshallingContext, Void> sessionManagerFactoryConfig = new HotRodSessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, HttpSessionBindingListener, MarshallingContext, Void>() {
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
                // Deployment name = host name + context path
                return context.getVirtualServerName() + context.getContextPath();
            }

            @Override
            public MarshalledValueFactory<MarshallingContext> getMarshalledValueFactory() {
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
                    return container.administration().getOrCreateCache(cacheName, this.getConfigurationName());
                }
            }

            @Override
            public String getConfigurationName() {
                return configurationName;
            }

            @Override
            public String getContainerName() {
                return containerName;
            }

            @Override
            public Immutability getImmutability() {
                return immutability;
            }

            @Override
            public SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener, HttpSessionBindingListener> getSpecificationProvider() {
                return SpringSpecificationProvider.INSTANCE;
            }
        };

        this.managerFactory = new HotRodSessionManagerFactory<>(sessionManagerFactoryConfig);

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
        ApplicationEventPublisher publisher = this.configuration.getEventPublisher();
        SessionExpirationListener expirationListener = new ImmutableSessionExpirationListener(publisher, context);

        SessionManagerConfiguration<ServletContext> sessionManagerConfiguration = new SessionManagerConfiguration<ServletContext>() {
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
        };
        this.manager = this.managerFactory.createSessionManager(sessionManagerConfiguration);
        Duration timeout = Duration.ofMinutes(context.getSessionTimeout());
        Optional<Duration> defaultTimeout = Optional.empty();
        try {
            this.manager.setDefaultMaxInactiveInterval(timeout);
        } catch (NoSuchMethodError error) {
            // Servlet version < 4.0
            defaultTimeout = Optional.of(timeout);
        }
        this.manager.start();
        this.repository = new DistributableSessionRepository<>(this.manager, defaultTimeout, publisher);
    }

    @Override
    public void destroy() throws Exception {
        this.manager.stop();
        this.managerFactory.close();
        this.container.stop();
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
    public DistributableSession<TransactionBatch> createSession() {
        return this.repository.createSession();
    }

    @Override
    public DistributableSession<TransactionBatch> findById(String id) {
        return this.repository.findById(id);
    }

    @Override
    public void deleteById(String id) {
        this.repository.deleteById(id);
    }

    @Override
    public void save(DistributableSession<TransactionBatch> session) {
        this.repository.save(session);
    }
}
