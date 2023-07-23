/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.spring.annotation;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.web.context.ServletContextAware;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.spring.SessionRepositoryConfiguration;
import org.wildfly.clustering.web.spring.EmptyIndexResolver;
import org.wildfly.clustering.web.spring.SessionMarshallerFactory;
import org.wildfly.clustering.web.spring.SessionPersistenceGranularity;

/**
 * Spring configuration bean for a distributable session repository.
 * @author Paul Ferraro
 */
public class HttpSessionConfiguration extends SpringHttpSessionConfiguration implements SessionRepositoryConfiguration, ServletContextAware, ApplicationEventPublisherAware, ImportAware, Consumer<AnnotationAttributes> {

	private final Class<? extends Annotation> annotationClass;
	private Integer maxActiveSessions = null;
	private SessionAttributePersistenceStrategy persistenceStrategy = SessionAttributePersistenceStrategy.COARSE;
	private Function<ClassLoader, ByteBufferMarshaller> marshallerFactory = SessionMarshallerFactory.JBOSS;
	private Supplier<String> identifierFactory = () -> UUID.randomUUID().toString();
	private ApplicationEventPublisher publisher;
	private ServletContext context;

	protected HttpSessionConfiguration(Class<? extends Annotation> annotationClass) {
		this.annotationClass = annotationClass;
	}

	@Override
	public Integer getMaxActiveSessions() {
		return this.maxActiveSessions;
	}

	@Override
	public SessionAttributePersistenceStrategy getPersistenceStrategy() {
		return this.persistenceStrategy;
	}

	@Override
	public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
		return this.marshallerFactory;
	}

	@Override
	public Supplier<String> getIdentifierFactory() {
		return this.identifierFactory;
	}

	@Override
	public ApplicationEventPublisher getEventPublisher() {
		return this.publisher;
	}

	@Override
	public ServletContext getServletContext() {
		return this.context;
	}

	@Override
	public Map<String, String> getIndexes() {
		return Collections.emptyMap();
	}

	@Override
	public IndexResolver<Session> getIndexResolver() {
		return EmptyIndexResolver.INSTANCE;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void setServletContext(ServletContext context) {
		super.setServletContext(context);
		this.context = context;
	}

	@Autowired(required = false)
	public void setGranularity(SessionPersistenceGranularity granularity) {
		this.persistenceStrategy = granularity.get();
	}

	@Autowired(required = false)
	public void setPersistenceStrategy(SessionAttributePersistenceStrategy persistenceStrategy) {
		this.persistenceStrategy = persistenceStrategy;
	}

	@Autowired(required = false)
	public void setMarshallerFactory(Function<ClassLoader, ByteBufferMarshaller> marshallerFactory) {
		this.marshallerFactory = marshallerFactory;
	}

	@Autowired(required = false)
	public void setMaxActiveSessions(Integer maxActiveSessions) {
		this.maxActiveSessions = maxActiveSessions;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata metadata) {
		Map<String, Object> attributeMap = metadata.getAnnotationAttributes(this.annotationClass.getName());
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
		this.accept(attributes);
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		AnnotationAttributes manager = attributes.getAnnotation("manager");
		int maxActiveSessions = manager.getNumber("maxActiveSessions").intValue();
		this.maxActiveSessions = (maxActiveSessions >= 0) ? Integer.valueOf(maxActiveSessions) : null;
		this.marshallerFactory = manager.getEnum("marshallerFactory");
		SessionPersistenceGranularity strategy = manager.getEnum("granularity");
		this.persistenceStrategy = strategy.get();
	}
}
