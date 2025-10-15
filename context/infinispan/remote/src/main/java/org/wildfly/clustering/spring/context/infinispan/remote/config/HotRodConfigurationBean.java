/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.infinispan.remote.config;

import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.context.infinispan.remote.HotRodConfiguration;
import org.wildfly.clustering.spring.context.infinispan.remote.MutableHotRodConfiguration;

/**
 * A Spring bean that configures and provides a HotRod configuration.
 * @author Paul Ferraro
 */
public class HotRodConfigurationBean implements MutableHotRodConfiguration {

	private URI uri;
	private Properties properties = new Properties();
	private String templateName = null;
	private String configuration = HotRodConfiguration.DEFAULT_CONFIGURATION;

	private StringValueResolver resolver = value -> value;

	/**
	 * Creates a HotRod configuration bean.
	 */
	public HotRodConfigurationBean() {
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
	public String getTemplate() {
		return this.templateName;
	}

	@Override
	public String getConfiguration() {
		return this.configuration;
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
	public void setConfiguration(String configuration) {
		this.configuration = this.resolver.resolveStringValue(configuration);
	}

	@Override
	public void setTemplate(String templateName) {
		this.templateName = this.resolver.resolveStringValue(templateName);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		AnnotationAttributes config = attributes.getAnnotation("config");
		this.setUri(config.getString("uri"));
		this.setTemplate(Optional.ofNullable(config.getString("template")).filter(Predicate.not(String::isEmpty)).orElse(null));
		this.setConfiguration(config.getString("configuration"));
		for (AnnotationAttributes property : config.getAnnotationArray("properties")) {
			this.setProperty(property.getString("name"), property.getString("value"));
		}
	}
}
