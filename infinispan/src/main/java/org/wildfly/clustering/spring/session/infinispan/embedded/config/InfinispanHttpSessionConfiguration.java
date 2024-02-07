/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.EmptyIndexResolver;
import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepository;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class InfinispanHttpSessionConfiguration extends InfinispanSessionRepositoryConfigurationBean {

	public InfinispanHttpSessionConfiguration() {
		super(Map.of(), EmptyIndexResolver.INSTANCE);
	}

	@Bean
	public InfinispanSessionRepository sessionRepository() {
		return new InfinispanSessionRepository(this);
	}
}
