/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.server.SpringWebSessionConfiguration;
import org.wildfly.clustering.spring.session.infinispan.remote.HotRodReactiveSessionRepository;
import org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodWebSessionConfiguration;

/**
 * Configuration for the {@link EnableHotRodWebSession} annotation.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringWebSessionConfiguration.class)
public class HotRodWebSessionAnnotationConfiguration extends HotRodSessionRepositoryAnnotationConfiguration {

	private final HotRodWebSessionConfiguration configuration;

	public HotRodWebSessionAnnotationConfiguration() {
		this(new HotRodWebSessionConfiguration());
	}

	private HotRodWebSessionAnnotationConfiguration(HotRodWebSessionConfiguration configuration) {
		super(configuration, EnableHotRodWebSession.class);
		this.configuration = configuration;
	}

	@Bean
	public HotRodReactiveSessionRepository sessionRepository() {
		return this.configuration.sessionRepository();
	}
}
