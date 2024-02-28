/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.util.OptionalInt;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.springframework.beans.factory.InitializingBean;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionManagerFactory;
import org.wildfly.clustering.spring.context.AutoDestroyBean;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagerFactoryBean<S, C, L> extends AutoDestroyBean implements SessionManagerFactory<C, Void, TransactionBatch>, InitializingBean {

	private final SessionManagerFactoryConfiguration<S, C, L, Void> configuration;
	private final HotRodConfiguration hotrod;
	private final RemoteCacheContainerProvider provider;

	private SessionManagerFactory<C, Void, TransactionBatch> sessionManagerFactory;

	public HotRodSessionManagerFactoryBean(SessionManagerFactoryConfiguration<S, C, L, Void> configuration, HotRodConfiguration hotrod, RemoteCacheContainerProvider provider) {
		this.hotrod = hotrod;
		this.provider = provider;
		this.configuration = configuration;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		RemoteCacheContainer container = this.provider.getRemoteCacheContainer();
		String deploymentName = this.configuration.getDeploymentName();
		String templateName = this.hotrod.getTemplateName();
		OptionalInt maxActiveSessions = this.configuration.getMaxActiveSessions();

		container.getConfiguration().addRemoteCache(deploymentName, builder -> builder.forceReturnValues(false).nearCacheMode(maxActiveSessions.isEmpty() ? NearCacheMode.DISABLED : NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE).templateName(templateName));

		int expirationThreadPoolSize = this.hotrod.getExpirationThreadPoolSize();
		RemoteCache<?, ?> cache = container.getCache(deploymentName);

		cache.start();
		this.accept(cache::stop);

		HotRodSessionFactoryConfiguration hotrodConfiguration = new HotRodSessionFactoryConfiguration() {
			@Override
			public int getExpirationThreadPoolSize() {
				return expirationThreadPoolSize;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <CK, CV> RemoteCache<CK, CV> getCache() {
				return (RemoteCache<CK, CV>) cache;
			}
		};

		this.sessionManagerFactory = new HotRodSessionManagerFactory<>(this.configuration, hotrodConfiguration);
		this.accept(this.sessionManagerFactory::close);
	}

	@Override
	public SessionManager<Void, TransactionBatch> createSessionManager(SessionManagerConfiguration<C> configuration) {
		return this.sessionManagerFactory.createSessionManager(configuration);
	}

	@Override
	public void close() {
		this.sessionManagerFactory.close();
	}
}
