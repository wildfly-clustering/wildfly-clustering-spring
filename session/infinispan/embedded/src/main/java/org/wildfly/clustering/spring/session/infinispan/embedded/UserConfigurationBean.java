/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.ServletContext;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheContainerConfiguration;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.embedded.user.InfinispanUserManagerFactory;
import org.wildfly.clustering.session.user.UserManager;
import org.wildfly.clustering.session.user.UserManagerConfiguration;
import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.clustering.spring.context.AutoDestroyBean;
import org.wildfly.clustering.spring.session.IndexingConfiguration;
import org.wildfly.clustering.spring.session.UserConfiguration;

/**
 * A Spring bean that configures and produces a user configuration.
 * @author Paul Ferraro
 */
public class UserConfigurationBean extends AutoDestroyBean implements UserConfiguration, InitializingBean {

	private final Map<String, UserManager<Void, Void, String, String>> managers = new TreeMap<>();
	private final SessionManagerFactoryConfiguration<Void> sessionManagerFactoryConfiguration;
	private final SessionManagerConfiguration<ServletContext> sessionManagerConfiguration;
	private final IndexingConfiguration indexing;
	private final EmbeddedCacheContainerConfiguration infinispan;

	/**
	 * Creates a user configuration bean
	 * @param managerFactoryConfiguration the session manager factory configuration
	 * @param managerConfiguration the session manager configuration
	 * @param indexing the indexing configuration
	 * @param infinispan the cache container configuration
	 */
	public UserConfigurationBean(SessionManagerFactoryConfiguration<Void> managerFactoryConfiguration, SessionManagerConfiguration<ServletContext> managerConfiguration, IndexingConfiguration indexing, EmbeddedCacheContainerConfiguration infinispan) {
		this.sessionManagerFactoryConfiguration = managerFactoryConfiguration;
		this.sessionManagerConfiguration = managerConfiguration;
		this.indexing = indexing;
		this.infinispan = infinispan;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		EmbeddedCacheManager container = this.infinispan.getCacheContainer();
		String applicationName = this.sessionManagerFactoryConfiguration.getDeploymentName();
		for (Map.Entry<String, String> entry : this.indexing.getIndexes().entrySet()) {
			String cacheName = String.format("%s/%s", applicationName, entry.getKey());
			String indexName = entry.getValue();

			ConfigurationBuilder builder = new ConfigurationBuilder().read(container.getCacheConfiguration(applicationName));
			container.defineConfiguration(cacheName, builder.build());
			this.accept(() -> container.undefineConfiguration(cacheName));

			Cache<?, ?> cache = container.getCache(cacheName);
			cache.start();
			this.accept(cache::stop);

			EmbeddedCacheConfiguration cacheConfiguration = new EmbeddedCacheConfiguration() {
				@Override
				public <K, V> Cache<K, V> getCache() {
					return container.getCache(cacheName);
				}
			};
			UserManagerFactory<Void, String, String> userManagerFactory = new InfinispanUserManagerFactory<>(cacheConfiguration);

			UserManager<Void, Void, String, String> userManager = userManagerFactory.createUserManager(new UserManagerConfiguration<>() {
				@Override
				public Supplier<String> getIdentifierFactory() {
					return UserConfigurationBean.this.sessionManagerConfiguration.getIdentifierFactory();
				}

				@Override
				public ByteBufferMarshaller getMarshaller() {
					return UserConfigurationBean.this.sessionManagerFactoryConfiguration.getMarshaller();
				}

				@Override
				public Supplier<Void> getTransientContextFactory() {
					return Supplier.of(null);
				}
			});
			this.managers.put(indexName, userManager);
		}
	}

	@Override
	public Map<String, UserManager<Void, Void, String, String>> getUserManagers() {
		return this.managers;
	}

	@Override
	public IndexResolver<Session> getIndexResolver() {
		return this.indexing.getIndexResolver();
	}
}
