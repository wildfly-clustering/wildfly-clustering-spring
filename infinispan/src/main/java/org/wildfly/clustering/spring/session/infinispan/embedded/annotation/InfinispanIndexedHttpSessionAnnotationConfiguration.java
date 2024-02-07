/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepository;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanIndexedHttpSessionConfiguration;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class InfinispanIndexedHttpSessionAnnotationConfiguration extends InfinispanSessionRepositoryAnnotationConfiguration {

	private final InfinispanIndexedHttpSessionConfiguration configuration;

	public InfinispanIndexedHttpSessionAnnotationConfiguration() {
		this(new InfinispanIndexedHttpSessionConfiguration());
	}

	private InfinispanIndexedHttpSessionAnnotationConfiguration(InfinispanIndexedHttpSessionConfiguration configuration) {
		super(configuration, EnableInfinispanIndexedHttpSession.class);
		this.configuration = configuration;
	}

	@Bean
	public InfinispanSessionRepository sessionRepository() {
		return this.configuration.sessionRepository();
	}
}
