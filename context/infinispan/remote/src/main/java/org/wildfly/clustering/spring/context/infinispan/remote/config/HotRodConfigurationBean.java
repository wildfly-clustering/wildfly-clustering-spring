/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.infinispan.remote.config;

import java.net.URI;
import java.util.Properties;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.context.infinispan.remote.MutableHotRodConfiguration;

/**
 * @author Paul Ferraro
 */
public class HotRodConfigurationBean implements MutableHotRodConfiguration {

	private URI uri;
	private Properties properties = new Properties();
	private String templateName = DefaultTemplate.DIST_SYNC.getTemplateName();
	private StringValueResolver resolver = value -> value;

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
	public void setUri(String uri) {
		this.uri = URI.create(this.resolver.resolveStringValue(uri));
	}

	@Override
	public void setProperty(String name, String value) {
		this.properties.setProperty(name, this.resolver.resolveStringValue(value));
	}

	@Override
	public void setTemplate(String templateName) {
		this.templateName = this.resolver.resolveStringValue(templateName);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		AnnotationAttributes config = attributes.getAnnotation("config");
		this.setUri(config.getString("uri"));
		this.setTemplate(config.getString("template"));
		for (AnnotationAttributes property : config.getAnnotationArray("properties")) {
			this.setProperty(property.getString("name"), property.getString("value"));
		}
	}
}
