/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.embedded.config;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.spec.servlet.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.session.spec.servlet.HttpSessionProvider;
import org.wildfly.clustering.spring.context.infinispan.embedded.EmbeddedCacheManagerBean;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagerFactoryBean;
import org.wildfly.clustering.spring.context.infinispan.embedded.MutableInfinispanConfiguration;
import org.wildfly.clustering.spring.context.infinispan.embedded.config.InfinispanConfigurationBean;
import org.wildfly.clustering.spring.web.config.WebSessionConfiguration;
import org.wildfly.clustering.spring.web.infinispan.embedded.config.annotation.EnableInfinispanWebSession;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class InfinispanWebSessionConfiguration extends WebSessionConfiguration implements MutableInfinispanConfiguration {

	private final MutableInfinispanConfiguration configuration = new InfinispanConfigurationBean();

	public InfinispanWebSessionConfiguration() {
		super(EnableInfinispanWebSession.class);
	}

	@Bean
	public ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration embeddedCacheManagerConfiguration() {
		return new EmbeddedCacheManagerBean(this);
	}

	@Bean
	public SessionManagerFactory<ServletContext, Void> sessionManagerFactory(ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration embeddedCacheManagerConfiguration) {
		return new InfinispanSessionManagerFactoryBean<>(this, HttpSessionProvider.INSTANCE, HttpSessionActivationListenerProvider.INSTANCE, this.configuration, embeddedCacheManagerConfiguration);
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
	public String getConfigurationResource() {
		return this.configuration.getConfigurationResource();
	}

	@Override
	public String getTemplateName() {
		return this.configuration.getTemplateName();
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		this.configuration.accept(attributes);
	}
}
