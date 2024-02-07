/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.server.SpringWebSessionConfiguration;
import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanReactiveSessionRepository;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanWebSessionConfiguration;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringWebSessionConfiguration.class)
public class InfinispanWebSessionAnnotationConfiguration extends InfinispanSessionRepositoryAnnotationConfiguration {

	private final InfinispanWebSessionConfiguration configuration;

	public InfinispanWebSessionAnnotationConfiguration() {
		this(new InfinispanWebSessionConfiguration());
	}

	private InfinispanWebSessionAnnotationConfiguration(InfinispanWebSessionConfiguration configuration) {
		super(configuration, EnableInfinispanWebSession.class);
		this.configuration = configuration;
	}

	@Bean
	public InfinispanReactiveSessionRepository sessionRepository() {
		return this.configuration.sessionRepository();
	}
}
