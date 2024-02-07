/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote.config;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.remote.HotRodSessionRepositoryConfiguration;

/**
 * Spring configuration bean for a session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 */
public class HotRodSessionRepositoryConfigurationBean extends SessionRepositoryConfigurationBean implements HotRodSessionRepositoryConfiguration, EmbeddedValueResolverAware {

	private URI uri;
	private Properties properties = new Properties();
	private String templateName = DefaultTemplate.DIST_SYNC.getTemplateName();
	private int expirationThreadPoolSize = 16;
	private StringValueResolver resolver = value -> value;

	protected HotRodSessionRepositoryConfigurationBean(Map<String, String> indexes, IndexResolver<Session> resolver) {
		super(indexes, resolver);
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	@Override
	public String getTemplateName() {
		return this.templateName;
	}

	@Override
	public int getExpirationThreadPoolSize() {
		return this.expirationThreadPoolSize;
	}

	@Autowired(required = false)
	public void setUri(String uri) {
		this.uri = URI.create(this.resolver.resolveStringValue(uri));
	}

	@Autowired(required = false)
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Autowired(required = false)
	public void setTemplateName(String templateName) {
		this.templateName = this.resolver.resolveStringValue(templateName);
	}

	@Autowired(required = false)
	public void setExpirationThreadPoolSize(int size) {
		this.expirationThreadPoolSize = size;
	}
}
