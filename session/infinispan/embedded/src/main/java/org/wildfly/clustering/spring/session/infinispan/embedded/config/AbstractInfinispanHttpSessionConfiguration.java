/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.config;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.spec.servlet.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.session.spec.servlet.HttpSessionProvider;
import org.wildfly.clustering.spring.context.infinispan.embedded.EmbeddedCacheManagerBean;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagerFactoryBean;
import org.wildfly.clustering.spring.context.infinispan.embedded.MutableInfinispanConfiguration;
import org.wildfly.clustering.spring.context.infinispan.embedded.config.InfinispanConfigurationBean;
import org.wildfly.clustering.spring.session.config.HttpSessionConfiguration;

/**
 * @author Paul Ferraro
 */
public class AbstractInfinispanHttpSessionConfiguration extends HttpSessionConfiguration implements MutableInfinispanConfiguration {

	private final MutableInfinispanConfiguration configuration = new InfinispanConfigurationBean();

	protected AbstractInfinispanHttpSessionConfiguration(Class<? extends Annotation> annotationClass, Map<String, String> defaultIndexes, IndexResolver<Session> defaultIndexResolver) {
		super(annotationClass, defaultIndexes, defaultIndexResolver);
	}

	@Bean
	public ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration embeddedCacheManagerConfiguration() {
		return new EmbeddedCacheManagerBean(this);
	}

	@Bean
	public SessionManagerFactory<ServletContext, Void, TransactionBatch> sessionManagerFactory(ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration embeddedCacheManagerConfiguration) {
		return new InfinispanSessionManagerFactoryBean<>(this, HttpSessionProvider.INSTANCE, HttpSessionActivationListenerProvider.INSTANCE, this.configuration, embeddedCacheManagerConfiguration);
	}

	@Override
	public String getConfigurationResource() {
		return this.configuration.getConfigurationResource();
	}

	@Override
	public String getTemplateName() {
		return this.configuration.getTemplateName();
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.configuration.setEmbeddedValueResolver(resolver);
	}

	@Override
	@Autowired(required = false)
	public void setResource(String resource) {
		this.configuration.setResource(resource);
	}

	@Override
	@Autowired(required = false)
	public void setTemplate(String templateName) {
		this.configuration.setTemplate(templateName);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		super.accept(attributes);
		this.configuration.accept(attributes);
	}
}
