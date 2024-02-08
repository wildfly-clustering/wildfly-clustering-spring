/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.infinispan.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanSessionRepositoryConfigurationBean;
import org.wildfly.clustering.web.spring.infinispan.InfinispanSessionRepository;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.embedded.annotation.EnableInfinispanIndexedHttpSession} instead.
 */
@Deprecated(forRemoval = true)
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class InfinispanIndexedHttpSessionConfiguration extends InfinispanSessionRepositoryConfigurationBean {

	public InfinispanIndexedHttpSessionConfiguration() {
		super(SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEXES, SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEX_RESOLVER);
	}

	@Bean
	public InfinispanSessionRepository sessionRepository() {
		return new InfinispanSessionRepository(this);
	}
}
