/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.hotrod.annotation;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.EmptyIndexResolver;
import org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodSessionRepositoryConfigurationBean;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepository;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.remote.config.HodRodHttpSessionConfiguration} instead.
 */
@Deprecated
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class HotRodHttpSessionConfiguration extends HotRodSessionRepositoryConfigurationBean {

	public HotRodHttpSessionConfiguration() {
		super(Map.of(), EmptyIndexResolver.INSTANCE);
	}

	@Bean
	public HotRodSessionRepository sessionRepository() {
		return new HotRodSessionRepository(this);
	}
}
