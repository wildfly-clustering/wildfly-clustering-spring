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
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.spring.context.infinispan.remote.HotRodSessionManagerFactoryBean;
import org.wildfly.clustering.spring.context.infinispan.remote.MutableHotRodConfiguration;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProvider;
import org.wildfly.clustering.spring.context.infinispan.remote.RemoteCacheContainerProviderBean;
import org.wildfly.clustering.spring.context.infinispan.remote.config.HotRodConfigurationBean;
import org.wildfly.clustering.spring.web.config.WebSessionConfiguration;
import org.wildfly.clustering.spring.web.infinispan.remote.config.annotation.EnableHotRodWebSession;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class HotRodWebSessionConfiguration extends WebSessionConfiguration implements MutableHotRodConfiguration {

	private final MutableHotRodConfiguration configuration = new HotRodConfigurationBean();

	public HotRodWebSessionConfiguration() {
		super(EnableHotRodWebSession.class);
	}

	@Bean
	public RemoteCacheContainerProvider remoteCacheManagerProvider() {
		return new RemoteCacheContainerProviderBean(this);
	}

	@Bean
	public SessionManagerFactory<ServletContext, Void, TransactionBatch> sessionManagerFactory(RemoteCacheContainerProvider provider) {
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
	public String getTemplateName() {
		return this.configuration.getTemplateName();
	}

	@Override
	public int getExpirationThreadPoolSize() {
		return this.configuration.getExpirationThreadPoolSize();
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
	public void setTemplateName(String templateName) {
		this.configuration.setTemplateName(templateName);
	}

	@Override
	@Autowired(required = false)
	public void setExpirationThreadPoolSize(int size) {
		this.configuration.setExpirationThreadPoolSize(size);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		this.configuration.accept(attributes);
	}
}
