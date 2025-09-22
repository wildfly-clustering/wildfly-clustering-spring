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
 * @author Paul Ferraro
 * @param <S> session type
 * @param <C> session manager context type
 * @param <L> session passivation listener type
 */
public class HotRodSessionManagerFactoryBean<S, C, L> extends AutoDestroyBean implements SessionManagerFactory<C, Void>, InitializingBean {

	private final SessionManagerFactoryConfiguration<Void> configuration;
	private final SessionSpecificationProvider<S, C> sessionProvider;
	private final SessionEventListenerSpecificationProvider<S, L> listenerProvider;
	private final HotRodConfiguration hotrod;
	private final RemoteCacheContainerProvider provider;

	private SessionManagerFactory<C, Void> sessionManagerFactory;

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
		String templateName = this.hotrod.getTemplateName();
		String configuration = this.hotrod.getConfiguration();
		OptionalInt maxActiveSessions = this.configuration.getMaxActiveSessions();

		container.getConfiguration().addRemoteCache(deploymentName, builder -> {
			builder.forceReturnValues(false).nearCacheMode(maxActiveSessions.isEmpty() ? NearCacheMode.DISABLED : NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE);
			if (templateName != null) {
				builder.templateName(templateName);
			} else {
				builder.configuration(configuration);
			}
		});

		RemoteCache<?, ?> cache = container.getCache(deploymentName);

		cache.start();
		this.accept(cache::stop);

		RemoteCacheConfiguration hotrodConfiguration = new RemoteCacheConfiguration() {
			@SuppressWarnings("unchecked")
			@Override
			public <CK, CV> RemoteCache<CK, CV> getCache() {
				return (RemoteCache<CK, CV>) cache.withDataFormat(DataFormat.builder().keyType(MediaType.APPLICATION_OBJECT).keyMarshaller(container.getMarshaller()).valueType(MediaType.APPLICATION_OBJECT).valueMarshaller(container.getMarshaller()).build());
			}
		};

		this.sessionManagerFactory = new HotRodSessionManagerFactory<>(this.configuration, this.sessionProvider, this.listenerProvider, hotrodConfiguration);
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
