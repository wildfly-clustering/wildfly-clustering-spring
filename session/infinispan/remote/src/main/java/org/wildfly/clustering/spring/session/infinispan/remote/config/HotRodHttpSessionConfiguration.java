/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.session.user.UserManager;
import org.wildfly.clustering.spring.session.EmptyIndexResolver;
import org.wildfly.clustering.spring.session.UserConfiguration;
import org.wildfly.clustering.spring.session.infinispan.remote.config.annotation.EnableHotRodHttpSession;

/**
 * A Spring bean that configures and produces a non-indexing Spring Session repository.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class HotRodHttpSessionConfiguration extends AbstractHotRodHttpSessionConfiguration {
	/**
	 * Creates a HotRod session configuration.
	 */
	public HotRodHttpSessionConfiguration() {
		super(EnableHotRodHttpSession.class, Map.of(), EmptyIndexResolver.INSTANCE);
	}

	/**
	 * Returns a user configuration.
	 * @return a user configuration
	 */
	@Bean
	public UserConfiguration userConfiguration() {
		return new UserConfiguration() {
			@Override
			public Map<String, UserManager<Void, Void, String, String>> getUserManagers() {
				return Map.of();
			}

			@Override
			public IndexResolver<Session> getIndexResolver() {
				return HotRodHttpSessionConfiguration.this.getIndexResolver();
			}
		};
	}
}
