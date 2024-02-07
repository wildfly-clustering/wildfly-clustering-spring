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
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanHttpSessionConfiguration;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class InfinispanHttpSessionAnnotationConfiguration extends InfinispanSessionRepositoryAnnotationConfiguration {

	private final InfinispanHttpSessionConfiguration configuration;

	public InfinispanHttpSessionAnnotationConfiguration() {
		this(new InfinispanHttpSessionConfiguration());
	}

	private InfinispanHttpSessionAnnotationConfiguration(InfinispanHttpSessionConfiguration configuration) {
		super(configuration, EnableInfinispanHttpSession.class);
		this.configuration = configuration;
	}

	@Bean
	public InfinispanSessionRepository sessionRepository() {
		return this.configuration.sessionRepository();
	}
}
