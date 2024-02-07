/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
