/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.util.OptionalInt;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.dataconversion.MediaType;
import org.springframework.beans.factory.InitializingBean;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionManagerFactory;
import org.wildfly.clustering.session.spec.SessionEventListenerSpecificationProvider;
import org.wildfly.clustering.session.spec.SessionSpecificationProvider;
import org.wildfly.clustering.spring.context.AutoDestroyBean;

/**
 * A Spring bean that configures and provides a remote Infinispan session manager.
 * @author Paul Ferraro
 * @param <S> the session specification type
 * @param <C> the deployment context type
 * @param <L> the session event listener specification type
 */
public class HotRodSessionManagerFactoryBean<S, C, L> extends AutoDestroyBean implements SessionManagerFactory<C, Void>, InitializingBean {

	private final SessionManagerFactoryConfiguration<Void> configuration;
	private final SessionSpecificationProvider<S, C> sessionProvider;
	private final SessionEventListenerSpecificationProvider<S, L> listenerProvider;
	private final HotRodConfiguration hotrod;
	private final RemoteCacheContainerProvider provider;

	private SessionManagerFactory<C, Void> sessionManagerFactory;

	/**
	 * Creates an Infinispan session manager bean.
	 * @param configuration the session manager factory configuration
	 * @param sessionProvider the session specification provider
	 * @param listenerProvider the session event listener specification provider
	 * @param hotrod a HotRod configuration
	 * @param provider a remote cache container provider
	 */
	public HotRodSessionManagerFactoryBean(SessionManagerFactoryConfiguration<Void> configuration, SessionSpecificationProvider<S, C> sessionProvider, SessionEventListenerSpecificationProvider<S, L> listenerProvider, HotRodConfiguration hotrod, RemoteCacheContainerProvider provider) {
		this.hotrod = hotrod;
		this.sessionProvider = sessionProvider;
		this.listenerProvider = listenerProvider;
		this.provider = provider;
		this.configuration = configuration;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		RemoteCacheContainer container = this.provider.getRemoteCacheContainer();
		String deploymentName = this.configuration.getDeploymentName();
		String templateName = this.hotrod.getTemplate();
		String configuration = this.hotrod.getConfiguration();
		OptionalInt maxSize = this.configuration.getMaxSize();

		container.getConfiguration().addRemoteCache(deploymentName, builder -> {
			builder.forceReturnValues(false).nearCacheMode(maxSize.isEmpty() ? NearCacheMode.DISABLED : NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE);
			if (templateName != null) {
				builder.templateName(templateName);
			} else {
				builder.configuration(configuration);
			}
		});

		RemoteCache<?, ?> cache = container.getCache(deploymentName);

		cache.start();
		this.accept(cache::stop);

		RemoteCacheConfiguration cacheConfiguration = new RemoteCacheConfiguration() {
			@SuppressWarnings("unchecked")
			@Override
			public <CK, CV> RemoteCache<CK, CV> getCache() {
				return (RemoteCache<CK, CV>) container.getCache(deploymentName).withDataFormat(DataFormat.builder().keyType(MediaType.APPLICATION_OBJECT).keyMarshaller(container.getMarshaller()).valueType(MediaType.APPLICATION_OBJECT).valueMarshaller(container.getMarshaller()).build());
			}
		};

		this.sessionManagerFactory = new HotRodSessionManagerFactory<>(new HotRodSessionManagerFactory.Configuration<S, C, Void, L>() {
			@Override
			public SessionManagerFactoryConfiguration<Void> getSessionManagerFactoryConfiguration() {
				return HotRodSessionManagerFactoryBean.this.configuration;
			}

			@Override
			public SessionSpecificationProvider<S, C> getSessionSpecificationProvider() {
				return HotRodSessionManagerFactoryBean.this.sessionProvider;
			}

			@Override
			public SessionEventListenerSpecificationProvider<S, L> getSessionEventListenerSpecificationProvider() {
				return HotRodSessionManagerFactoryBean.this.listenerProvider;
			}

			@Override
			public RemoteCacheConfiguration getCacheConfiguration() {
				return cacheConfiguration;
			}
		});
		this.accept(this::close);
	}

	@Override
	public SessionManager<Void> createSessionManager(SessionManagerConfiguration<C> configuration) {
		return this.sessionManagerFactory.createSessionManager(configuration);
	}

	@Override
	public void close() {
		this.sessionManagerFactory.close();
	}
}
