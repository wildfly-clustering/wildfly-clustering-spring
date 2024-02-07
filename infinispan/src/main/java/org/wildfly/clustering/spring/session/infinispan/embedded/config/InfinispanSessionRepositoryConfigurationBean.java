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

package org.wildfly.clustering.spring.session.infinispan.embedded.config;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepositoryConfiguration;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionRepositoryConfigurationBean extends SessionRepositoryConfigurationBean implements InfinispanSessionRepositoryConfiguration, EmbeddedValueResolverAware {

	private String resource = "/WEB-INF/infinispan.xml";
	private String templateName = null;
	private StringValueResolver resolver = value -> value;

	protected InfinispanSessionRepositoryConfigurationBean(Map<String, String> indexes, IndexResolver<Session> indexResolver) {
		super(indexes, indexResolver);
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public String getConfigurationResource() {
		return this.resource;
	}

	@Override
	public String getTemplateName() {
		return this.templateName;
	}

	@Autowired(required = false)
	public void setConfigurationResource(String resource) {
		this.resource = this.resolver.resolveStringValue(resource);
	}

	@Autowired(required = false)
	public void setTemplateName(String templateName) {
		this.templateName = Optional.ofNullable(templateName).map(this.resolver::resolveStringValue).orElse(null);
	}
}
