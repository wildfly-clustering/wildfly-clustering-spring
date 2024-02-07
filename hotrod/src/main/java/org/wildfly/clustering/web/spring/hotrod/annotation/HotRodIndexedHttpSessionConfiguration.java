/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.spring.hotrod.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodSessionRepositoryConfigurationBean;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepository;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodIndexedHttpSessionConfiguration} instead.
 */
@Deprecated
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class HotRodIndexedHttpSessionConfiguration extends HotRodSessionRepositoryConfigurationBean {

	public HotRodIndexedHttpSessionConfiguration() {
		super(SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEXES, SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEX_RESOLVER);
	}

	@Bean
	public HotRodSessionRepository sessionRepository() {
		return new HotRodSessionRepository(this);
	}
}
