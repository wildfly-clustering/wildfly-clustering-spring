/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded.config;

import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.context.infinispan.embedded.MutableInfinispanConfiguration;

/**
 * @author Paul Ferraro
 */
public class InfinispanConfigurationBean implements MutableInfinispanConfiguration {

	private String resource = DEFAULT_CONFIGURATION_RESOURCE;
	private String templateName = null;
	private StringValueResolver resolver = value -> value;

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

	@Override
	public void setConfigurationResource(String resource) {
		this.resource = this.resolver.resolveStringValue(resource);
	}

	@Override
	public void setTemplateName(String templateName) {
		this.templateName = Optional.ofNullable(templateName).map(this.resolver::resolveStringValue).orElse(null);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		AnnotationAttributes config = attributes.getAnnotation("config");
		this.setConfigurationResource(config.getString("resource"));
		this.setTemplateName(Optional.of(config.getString("template")).filter(Predicate.not(String::isEmpty)).orElse(null));
	}
}
