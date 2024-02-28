/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.remote;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
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
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class UserConfigurationBean extends AutoDestroyBean implements UserConfiguration<TransactionBatch>, InitializingBean, ApplicationContextAware {

	private final Map<String, UserManager<Void, Void, String, String, TransactionBatch>> managers = new TreeMap<>();
	private final SessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, Void> managerFactoryConfiguration;
	private final SessionManagerConfiguration<ServletContext> managerConfiguration;
	private final IndexingConfiguration indexing;
	private final HotRodConfiguration hotrod;
	private final RemoteCacheContainerProvider provider;

	private ApplicationContext context;

	public UserConfigurationBean(SessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, Void> managerFactoryConfiguration, SessionManagerConfiguration<ServletContext> managerConfiguration, IndexingConfiguration indexing, HotRodConfiguration hotrod, RemoteCacheContainerProvider provider) {
		this.managerFactoryConfiguration = managerFactoryConfiguration;
		this.managerConfiguration = managerConfiguration;
		this.indexing = indexing;
		this.hotrod = hotrod;
		this.provider = provider;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		RemoteCacheContainer container = this.provider.getRemoteCacheContainer();
		String applicationName = this.context.getApplicationName();
		String templateName = this.hotrod.getTemplateName();
		for (Map.Entry<String, String> entry : this.indexing.getIndexes().entrySet()) {
			String cacheName = String.format("%s/%s", applicationName, entry.getKey());
			String indexName = entry.getValue();

			container.getConfiguration().addRemoteCache(cacheName, builder -> builder.forceReturnValues(false).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NONE).templateName(templateName));

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
					return UserConfigurationBean.this.managerConfiguration.getIdentifierFactory();
				}

				@Override
				public ByteBufferMarshaller getMarshaller() {
					return UserConfigurationBean.this.managerFactoryConfiguration.getMarshaller();
				}

				@Override
				public Supplier<Void> getTransientContextFactory() {
					return Functions.constantSupplier(null);
				}
			});
			this.managers.put(indexName, userManager);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	@Override
	public Map<String, UserManager<Void, Void, String, String, TransactionBatch>> getUserManagers() {
		return this.managers;
	}

	@Override
	public IndexResolver<Session> getIndexResolver() {
		return this.indexing.getIndexResolver();
	}
}
