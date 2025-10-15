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
import org.wildfly.clustering.spring.web.config.WebSessionManagerConfiguration;
import org.wildfly.clustering.spring.web.infinispan.embedded.config.annotation.EnableInfinispanWebSession;

/**
 * A Spring bean that configures and produces a Spring Web session manager.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class InfinispanWebSessionConfiguration extends WebSessionManagerConfiguration implements MutableInfinispanConfiguration {

	private final MutableInfinispanConfiguration configuration = new InfinispanConfigurationBean();

	/**
	 * Creates a web session configuration.
	 */
	public InfinispanWebSessionConfiguration() {
		super(EnableInfinispanWebSession.class);
	}

	/**
	 * Produces a command dispatcher factory configuration.
	 * @return a command dispatcher factory configuration.
	 */
	@Bean
	public ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration commandDispatcherFactoryConfiguration() {
		return new EmbeddedCacheManagerBean(this);
	}

	/**
	 * Produces a session manager factory.
	 * @param configuration a command dispatcher factory configuration
	 * @return a session manager factory
	 */
	@Bean
	public SessionManagerFactory<ServletContext, Void> sessionManagerFactory(ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration configuration) {
		return new InfinispanSessionManagerFactoryBean<>(this, HttpSessionProvider.INSTANCE, HttpSessionActivationListenerProvider.INSTANCE, this.configuration, configuration);
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
	public String getResource() {
		return this.configuration.getResource();
	}

	@Override
	public String getTemplate() {
		return this.configuration.getTemplate();
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		this.configuration.accept(attributes);
	}
}
