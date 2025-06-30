/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.remote.config;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.spec.servlet.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.session.spec.servlet.HttpSessionProvider;
import org.wildfly.clustering.spring.context.infinispan.remote.HotRodSessionManagerFactoryBean;
import org.wildfly.clustering.spring.context.infinispan.remote.MutableHotRodConfiguration;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProvider;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProviderBean;
import org.wildfly.clustering.spring.context.infinispan.remote.config.HotRodConfigurationBean;
import org.wildfly.clustering.spring.session.config.HttpSessionConfiguration;

/**
 * @author Paul Ferraro
 */
public class AbstractHotRodHttpSessionConfiguration extends HttpSessionConfiguration implements MutableHotRodConfiguration {

	private final MutableHotRodConfiguration configuration = new HotRodConfigurationBean();

	protected AbstractHotRodHttpSessionConfiguration(Class<? extends Annotation> annotationClass, Map<String, String> defaultIndexes, IndexResolver<Session> defaultIndexResolver) {
		super(annotationClass, defaultIndexes, defaultIndexResolver);
	}

	@Bean
	public RemoteCacheContainerProvider remoteCacheManagerProvider() {
		return new RemoteCacheContainerProviderBean(this);
	}

	@Bean
	public SessionManagerFactory<ServletContext, Void> sessionManagerFactory(RemoteCacheContainerProvider provider) {
		return new HotRodSessionManagerFactoryBean<>(this, HttpSessionProvider.INSTANCE, HttpSessionActivationListenerProvider.INSTANCE, this.configuration, provider);
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.configuration.setEmbeddedValueResolver(resolver);
	}

	@Override
	public URI getUri() {
		return this.configuration.getUri();
	}

	@Override
	public Properties getProperties() {
		return this.configuration.getProperties();
	}

	@Override
	public String getTemplateName() {
		return this.configuration.getTemplateName();
	}

	@Override
	public String getConfiguration() {
		return this.configuration.getConfiguration();
	}

	@Override
	@Autowired(required = false)
	public void setUri(String uri) {
		this.configuration.setUri(uri);
	}

	@Override
	@Autowired(required = false)
	public void setProperties(Properties properties) {
		this.configuration.setProperties(properties);
	}

	@Override
	public void setProperty(String name, String value) {
		this.configuration.setProperty(name, value);
	}

	@Override
	@Autowired(required = false)
	public void setConfiguration(String configuration) {
		this.configuration.setConfiguration(configuration);
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
