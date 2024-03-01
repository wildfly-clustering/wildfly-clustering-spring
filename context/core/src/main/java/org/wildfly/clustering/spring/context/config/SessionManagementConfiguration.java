/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.config;

import java.lang.annotation.Annotation;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.spring.context.SessionManagerBean;
import org.wildfly.clustering.spring.context.SessionMarshallerFactory;
import org.wildfly.common.function.Functions;

/**
 * Spring configuration bean for a distributable session repository.
 * @author Paul Ferraro
 */
public abstract class SessionManagementConfiguration<S, C, L> implements SessionManagerFactoryConfiguration<S, C, L, Void>, SessionManagerConfiguration<C>, EnvironmentAware, ImportAware, ResourceLoaderAware, Consumer<AnnotationAttributes> {

	private final Class<? extends Annotation> annotationClass;

	private IdGenerator generator = new JdkIdGenerator();
	private OptionalInt maxActiveSessions = OptionalInt.empty();
	private SessionAttributePersistenceStrategy persistenceStrategy = SessionAttributePersistenceStrategy.COARSE;
	private BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory = SessionMarshallerFactory.JAVA;
	private Environment environment;
	private ResourceLoader loader;

	protected SessionManagementConfiguration(Class<? extends Annotation> annotationClass) {
		this.annotationClass = annotationClass;
	}

	@Bean
	public <B extends Batch> SessionManager<Void, B> sessionManager(SessionManagerFactory<C, Void, B> factory) {
		return new SessionManagerBean<>(factory.createSessionManager(this));
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Supplier<Void> getSessionContextFactory() {
		return Functions.constantSupplier(null);
	}

	@Override
	public Immutability getImmutability() {
		return Immutability.getDefault();
	}

	@Override
	public Supplier<String> getIdentifierFactory() {
		return () -> this.generator.generateId().toString();
	}

	@Override
	public OptionalInt getMaxActiveSessions() {
		return this.maxActiveSessions;
	}

	@Override
	public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
		return this.persistenceStrategy;
	}

	@Override
	public ByteBufferMarshaller getMarshaller() {
		return this.marshallerFactory.apply(this.environment, this.loader);
	}

	@Autowired(required = false)
	public void setSessionIdentifierGenerator(IdGenerator generator) {
		this.generator = generator;
	}

	@Autowired(required = false)
	public void setGranularity(Supplier<SessionAttributePersistenceStrategy> granularity) {
		this.persistenceStrategy = granularity.get();
	}

	@Autowired(required = false)
	public void setPersistenceStrategy(SessionAttributePersistenceStrategy persistenceStrategy) {
		this.persistenceStrategy = persistenceStrategy;
	}

	@Autowired(required = false)
	public void setMarshallerFactory(BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory) {
		this.marshallerFactory = marshallerFactory;
	}

	@Autowired(required = false)
	public void setMaxActiveSessions(int maxActiveSessions) {
		this.maxActiveSessions = (maxActiveSessions >= 0) ? OptionalInt.of(maxActiveSessions) : OptionalInt.empty();
	}

	@Override
	public void setImportMetadata(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(this.annotationClass.getName()));
		AnnotationAttributes manager = attributes.getAnnotation("manager");
		this.setMaxActiveSessions(manager.getNumber("maxActiveSessions").intValue());
		this.setMarshallerFactory(manager.getEnum("marshallerFactory"));
		this.setGranularity(manager.getEnum("granularity"));
		this.accept(attributes);
	}
}
