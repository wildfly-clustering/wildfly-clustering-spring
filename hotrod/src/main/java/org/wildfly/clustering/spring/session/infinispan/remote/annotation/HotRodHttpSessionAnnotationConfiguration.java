/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.infinispan.remote.HotRodSessionRepository;
import org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodHttpSessionConfiguration;

/**
 * Configuration for the {@link EnableHotRodHttpSession} annotation.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class HotRodHttpSessionAnnotationConfiguration extends HotRodSessionRepositoryAnnotationConfiguration {

	private final HotRodHttpSessionConfiguration configuration;

	public HotRodHttpSessionAnnotationConfiguration() {
		this(new HotRodHttpSessionConfiguration());
	}

	private HotRodHttpSessionAnnotationConfiguration(HotRodHttpSessionConfiguration configuration) {
		super(configuration, EnableHotRodHttpSession.class);
		this.configuration = configuration;
	}

	@Bean
	public HotRodSessionRepository sessionRepository() {
		return this.configuration.sessionRepository();
	}
}
