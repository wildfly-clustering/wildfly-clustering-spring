/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded.annotation;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.session.annotation.SessionRepositoryAnnotationConfiguration;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanSessionRepositoryConfigurationBean;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionRepositoryAnnotationConfiguration extends SessionRepositoryAnnotationConfiguration implements EmbeddedValueResolverAware {

	private final InfinispanSessionRepositoryConfigurationBean configuration;

	InfinispanSessionRepositoryAnnotationConfiguration(InfinispanSessionRepositoryConfigurationBean configuration, Class<? extends Annotation> annotationClass) {
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
		this.configuration.setConfigurationResource(config.getString("resource"));
		this.configuration.setTemplateName(Optional.of(config.getString("template")).filter(Predicate.not(String::isEmpty)).orElse(null));
	}
}
