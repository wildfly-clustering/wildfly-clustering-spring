/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.remote;

import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.ServletContext;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.dataconversion.MediaType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.remote.user.HotRodUserManagerFactory;
import org.wildfly.clustering.session.user.UserManager;
import org.wildfly.clustering.session.user.UserManagerConfiguration;
import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.clustering.spring.context.AutoDestroyBean;
import org.wildfly.clustering.spring.context.infinispan.remote.HotRodConfiguration;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProvider;
import org.wildfly.clustering.spring.session.IndexingConfiguration;
import org.wildfly.clustering.spring.session.UserConfiguration;

/**
 * A Spring bean that configures and produces user configuration.
 * @author Paul Ferraro
 */
public class UserConfigurationBean extends AutoDestroyBean implements UserConfiguration, InitializingBean {

	private final Map<String, UserManager<Void, Void, String, String>> managers = new TreeMap<>();
	private final SessionManagerFactoryConfiguration<Void> managerFactoryConfiguration;
	private final SessionManagerConfiguration<ServletContext> managerConfiguration;
	private final IndexingConfiguration indexing;
	private final HotRodConfiguration hotrod;
	private final RemoteCacheContainerProvider provider;

	/**
	 * Creates a user configuration bean.
	 * @param managerFactoryConfiguration a session manager factory configuration
	 * @param managerConfiguration a session manager configuration
	 * @param indexing the indexing configuration
	 * @param hotrod the HotRod configuration
	 * @param provider the remote cache container provider
	 */
	public UserConfigurationBean(SessionManagerFactoryConfiguration<Void> managerFactoryConfiguration, SessionManagerConfiguration<ServletContext> managerConfiguration, IndexingConfiguration indexing, HotRodConfiguration hotrod, RemoteCacheContainerProvider provider) {
		this.managerFactoryConfiguration = managerFactoryConfiguration;
		this.managerConfiguration = managerConfiguration;
		this.indexing = indexing;
		this.hotrod = hotrod;
		this.provider = provider;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		RemoteCacheContainer container = this.provider.getRemoteCacheContainer();
		String applicationName = this.managerFactoryConfiguration.getDeploymentName();
		String templateName = this.hotrod.getTemplate();
		String configuration = this.hotrod.getConfiguration();
		for (Map.Entry<String, String> entry : this.indexing.getIndexes().entrySet()) {
			String cacheName = String.format("%s/%s", applicationName, entry.getKey());
			String indexName = entry.getValue();

			container.getConfiguration().addRemoteCache(cacheName, builder -> {
				builder.forceReturnValues(false).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NONE);
				if (templateName != null) {
					builder.templateName(templateName);
				} else {
					builder.configuration(configuration);
				}
			});

			RemoteCache<?, ?> cache = container.getCache(cacheName);

			cache.start();
			this.accept(cache::stop);

			RemoteCacheConfiguration cacheConfiguration = new RemoteCacheConfiguration() {
				@SuppressWarnings("unchecked")
				@Override
				public <CK, CV> RemoteCache<CK, CV> getCache() {
					return (RemoteCache<CK, CV>) cache.withDataFormat(DataFormat.builder().keyType(MediaType.APPLICATION_OBJECT).keyMarshaller(container.getMarshaller()).valueType(MediaType.APPLICATION_OBJECT).valueMarshaller(container.getMarshaller()).build());
				}
			};
			UserManagerFactory<Void, String, String> userManagerFactory = new HotRodUserManagerFactory<>(cacheConfiguration);

			UserManager<Void, Void, String, String> userManager = userManagerFactory.createUserManager(new UserManagerConfiguration<>() {
				@Override
				public Supplier<String> getIdentifierFactory() {
					return UserConfigurationBean.this.managerConfiguration.getIdentifierFactory();
				}

				@Override
				public ByteBufferMarshaller getMarshaller() {
					return UserConfigurationBean.this.managerFactoryConfiguration.getMarshaller();
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
