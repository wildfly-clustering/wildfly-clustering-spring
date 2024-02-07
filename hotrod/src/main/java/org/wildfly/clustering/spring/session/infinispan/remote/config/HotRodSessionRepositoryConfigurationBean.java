/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
