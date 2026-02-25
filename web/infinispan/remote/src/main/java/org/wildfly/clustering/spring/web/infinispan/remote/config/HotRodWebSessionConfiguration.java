/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.remote.config;

import java.net.URI;
import java.util.Properties;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.spring.context.infinispan.remote.HotRodSessionManagerFactoryBean;
import org.wildfly.clustering.spring.context.infinispan.remote.MutableHotRodConfiguration;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProvider;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProviderBean;
import org.wildfly.clustering.spring.context.infinispan.remote.config.HotRodConfigurationBean;
import org.wildfly.clustering.spring.web.config.WebSessionManagerConfiguration;
import org.wildfly.clustering.spring.web.infinispan.remote.config.annotation.EnableHotRodWebSession;

/**
 * A Spring bean that configures and produces a Spring Web session manager.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class HotRodWebSessionConfiguration extends WebSessionManagerConfiguration implements MutableHotRodConfiguration {

	private final MutableHotRodConfiguration configuration = new HotRodConfigurationBean();

	/**
	 * Creates a web session configuration.
	 */
	public HotRodWebSessionConfiguration() {
		super(EnableHotRodWebSession.class);
	}

	/**
	 * Produces a remote cache container provider.
	 * @return a remote cache container provider.
	 */
	@Bean
	public RemoteCacheContainerProvider remoteCacheManagerProvider() {
		return new RemoteCacheContainerProviderBean(this);
	}

	/**
	 * Produces a session manager factory.
	 * @param provider a remote cache container provider
	 * @return the session manager factory.
	 */
	@Bean
	public SessionManagerFactory<ServletContext, Void> sessionManagerFactory(RemoteCacheContainerProvider provider) {
		return new HotRodSessionManagerFactoryBean<>(this, this.configuration, provider);
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
	public String getTemplate() {
		return this.configuration.getTemplate();
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
		this.configuration.accept(attributes);
	}
}
