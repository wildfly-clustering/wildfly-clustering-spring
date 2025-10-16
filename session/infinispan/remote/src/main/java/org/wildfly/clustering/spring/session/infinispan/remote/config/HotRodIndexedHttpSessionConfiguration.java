/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.remote.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProvider;
import org.wildfly.clustering.spring.session.UserConfiguration;
import org.wildfly.clustering.spring.session.infinispan.remote.UserConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.remote.config.annotation.EnableHotRodIndexedHttpSession;

/**
 * A Spring bean that configures and produces an indexing Spring Session repository.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class HotRodIndexedHttpSessionConfiguration extends AbstractHotRodHttpSessionConfiguration {
	/**
	 * Creates an indexed session configuration.
	 */
	public HotRodIndexedHttpSessionConfiguration() {
		super(EnableHotRodIndexedHttpSession.class, DEFAULT_SPRING_SECURITY_INDEXES, DEFAULT_SPRING_SECURITY_INDEX_RESOLVER);
	}

	/**
	 * Returns a user configuration.
	 * @param provider a provider of a remote cache container
	 * @return a user configuration.
	 */
	@Bean
	public UserConfiguration userConfiguration(RemoteCacheContainerProvider provider) {
		return new UserConfigurationBean(this, this, this, this, provider);
	}

	@Autowired(required = false)
	@Override
	public void setIndexes(Map<String, String> indexes) {
		super.setIndexes(indexes);
	}

	@Autowired(required = false)
	@Override
	public void setIndexResolver(IndexResolver<Session> resolver) {
		super.setIndexResolver(resolver);
	}
}
