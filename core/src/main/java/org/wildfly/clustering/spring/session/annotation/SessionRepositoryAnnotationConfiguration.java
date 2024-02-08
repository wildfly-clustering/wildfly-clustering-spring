/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.web.context.ServletContextAware;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;

/**
 * @author Paul Ferraro
 */
public abstract class SessionRepositoryAnnotationConfiguration implements ServletContextAware, ApplicationEventPublisherAware, ImportAware, Consumer<AnnotationAttributes> {
	private final Class<? extends Annotation> annotationClass;
	private final SessionRepositoryConfigurationBean configuration;

	protected SessionRepositoryAnnotationConfiguration(SessionRepositoryConfigurationBean configuration, Class<? extends Annotation> annotationClass) {
		this.configuration = configuration;
		this.annotationClass = annotationClass;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.configuration.setApplicationEventPublisher(publisher);
	}

	@Override
	public void setServletContext(ServletContext context) {
		this.configuration.setServletContext(context);
	}

	@Override
	public void setImportMetadata(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(this.annotationClass.getName()));
		AnnotationAttributes manager = attributes.getAnnotation("manager");
		this.configuration.setMaxActiveSessions(manager.getNumber("maxActiveSessions").intValue());
		this.configuration.setMarshallerFactory(manager.getEnum("marshallerFactory"));
		this.configuration.setGranularity(manager.getEnum("granularity"));
		if (attributes.containsKey("indexing")) {
			AnnotationAttributes indexing = attributes.getAnnotation("indexing");
			Map<String, String> indexes = new TreeMap<>();
			for (AnnotationAttributes index : indexing.getAnnotationArray("indexes")) {
				indexes.put(index.getString("id"), index.getString("name"));
			}
			this.configuration.setIndexes(indexes);
			Class<? extends IndexResolver<Session>> resolverClass = indexing.getClass("resolverClass");
			try {
				this.configuration.setIndexResolver(resolverClass.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new IllegalArgumentException(resolverClass.getCanonicalName());
			}
		}
		this.accept(attributes);
	}
}
