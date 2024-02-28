/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheContainerConfiguration;
import org.wildfly.clustering.spring.session.UserConfiguration;
import org.wildfly.clustering.spring.session.infinispan.embedded.UserConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.annotation.EnableInfinispanIndexedHttpSession;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class InfinispanIndexedHttpSessionConfiguration extends AbstractInfinispanHttpSessionConfiguration {

	public InfinispanIndexedHttpSessionConfiguration() {
		super(EnableInfinispanIndexedHttpSession.class, DEFAULT_SPRING_SECURITY_INDEXES, DEFAULT_SPRING_SECURITY_INDEX_RESOLVER);
	}

	@Bean
	public UserConfiguration<TransactionBatch> userConfiguration(EmbeddedCacheContainerConfiguration configuration) {
		return new UserConfigurationBean(this, this, this, configuration);
	}

	@Autowired(required = false)
	@Override
	public void setIndexes(Map<String, String> indexes) {
		super.setIndexes(indexes);
	}

	@Autowired(required = false)
	@Override
	public void setIndexResolver(IndexResolver<Session> resolver) {
		super.setIndexResolver(resolver);
	}
}
