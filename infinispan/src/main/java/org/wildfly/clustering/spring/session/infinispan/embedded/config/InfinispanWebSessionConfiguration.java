/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.server.SpringWebSessionConfiguration;
import org.wildfly.clustering.spring.session.EmptyIndexResolver;
import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanReactiveSessionRepository;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringWebSessionConfiguration.class)
public class InfinispanWebSessionConfiguration extends InfinispanSessionRepositoryConfigurationBean {

	public InfinispanWebSessionConfiguration() {
		super(Map.of(), EmptyIndexResolver.INSTANCE);
	}

	@Bean
	public InfinispanReactiveSessionRepository sessionRepository() {
		return new InfinispanReactiveSessionRepository(this);
	}
}
