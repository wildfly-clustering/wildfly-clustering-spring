/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.remote.annotation;

import java.lang.annotation.Annotation;
import java.util.Properties;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.session.annotation.SessionRepositoryAnnotationConfiguration;
import org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodSessionRepositoryConfigurationBean;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionRepositoryAnnotationConfiguration extends SessionRepositoryAnnotationConfiguration implements EmbeddedValueResolverAware {

	private final HotRodSessionRepositoryConfigurationBean configuration;

	HotRodSessionRepositoryAnnotationConfiguration(HotRodSessionRepositoryConfigurationBean configuration, Class<? extends Annotation> annotationClass) {
		super(configuration, annotationClass);
		this.configuration = configuration;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.configuration.setEmbeddedValueResolver(resolver);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		AnnotationAttributes config = attributes.getAnnotation("config");
		this.configuration.setUri(config.getString("uri"));
		this.configuration.setTemplateName(config.getString("template"));
		Properties properties = new Properties();
		for (AnnotationAttributes property : config.getAnnotationArray("properties")) {
			properties.setProperty(property.getString("name"), property.getString("value"));
		}
		this.configuration.setProperties(properties);
	}
}
