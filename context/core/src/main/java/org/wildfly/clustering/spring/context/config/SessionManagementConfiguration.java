/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.config;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionManagerBean;

/**
 * Spring configuration bean for a session manager.
 * @author Paul Ferraro
 * @param <C> session manager context type
 */
public abstract class SessionManagementConfiguration<C> implements SessionManagerFactoryConfiguration<Void>, SessionManagerConfiguration<C>, EnvironmentAware, ImportAware, ResourceLoaderAware, Consumer<AnnotationAttributes> {

	private final Class<? extends Annotation> annotationClass;

	private IdGenerator generator = new JdkIdGenerator();
	private OptionalInt maxActiveSessions = OptionalInt.empty();
	private Optional<Duration> idleTimeout = Optional.empty();
	private SessionAttributePersistenceStrategy persistenceStrategy = SessionAttributePersistenceStrategy.COARSE;
	private BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory = SessionAttributeMarshaller.JAVA;
	private Environment environment;
	private ResourceLoader loader;

	/**
	 * Creates a session management configuration bean.
	 * @param annotationClass the class of the associated configuration annotation
	 */
	protected SessionManagementConfiguration(Class<? extends Annotation> annotationClass) {
		this.annotationClass = annotationClass;
	}

	/**
	 * Creates a session manager from the specified factory.
	 * @param factory a session manager factory
	 * @return a session manager
	 */
	@Bean
	public SessionManager<Void> sessionManager(SessionManagerFactory<C, Void> factory) {
		return new SessionManagerBean(factory.createSessionManager(this));
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
		return Supplier.of(null);
	}

	@Override
	public Immutability getImmutability() {
		return Immutability.getDefault();
	}

	@Override
	public Supplier<String> getIdentifierFactory() {
		Supplier<UUID> factory = this.generator::generateId;
		return factory.thenApply(UUID::toString);
	}

	@Override
	public OptionalInt getMaxSize() {
		return this.maxActiveSessions;
	}

	@Override
	public Optional<Duration> getIdleTimeout() {
		return this.idleTimeout;
	}

	@Override
	public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
		return this.persistenceStrategy;
	}

	@Override
	public ByteBufferMarshaller getMarshaller() {
		return this.marshallerFactory.apply(this.environment, this.loader);
	}

	/**
	 * Configures the session identifier generator for a session manager.
	 * @param generator a session identifier generator for a session manager.
	 */
	@Autowired(required = false)
	public void setSessionIdentifierGenerator(IdGenerator generator) {
		this.generator = generator;
	}

	/**
	 * Configures the session attribute persistence strategy for a session manager.
	 * @param granularity a provider of a session attribute persistence strategy
	 */
	@Autowired(required = false)
	public void setGranularity(Supplier<SessionAttributePersistenceStrategy> granularity) {
		this.persistenceStrategy = granularity.get();
	}

	/**
	 * Configures the session attribute persistence strategy for a session manager.
	 * @param persistenceStrategy an session attribute persistence strategy
	 */
	@Autowired(required = false)
	public void setPersistenceStrategy(SessionAttributePersistenceStrategy persistenceStrategy) {
		this.persistenceStrategy = persistenceStrategy;
	}

	/**
	 * Configures the session attribute marshaller for a session manager.
	 * @param marshallerFactory a factory for creating a session attribute marshaller
	 */
	@Autowired(required = false)
	public void setMarshaller(BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory) {
		this.marshallerFactory = marshallerFactory;
	}

	/**
	 * Configures the maximum number of sessions to retain in memory.
	 * @param maxActiveSessions the maximum number of sessions to retain in memory.
	 */
	@Autowired(required = false)
	public void setMaxActiveSessions(int maxActiveSessions) {
		this.maxActiveSessions = (maxActiveSessions >= 0) || (maxActiveSessions == Integer.MAX_VALUE) ? OptionalInt.of(maxActiveSessions) : OptionalInt.empty();
	}

	/**
	 * Configures the duration of time, expressed in ISO-8601 format, after which an idle session should passivate.
	 * @param idleTimeout the duration of time, expressed in ISO-8601 format, after which an idle session should passivate.
	 */
	@Autowired(required = false)
	public void setIdleTimeout(String idleTimeout) {
		this.idleTimeout = Optional.of(Duration.parse(idleTimeout)).filter(Predicate.not(Duration::isNegative).and(Predicate.not(Duration::isZero)));
	}

	@Override
	public void setImportMetadata(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(this.annotationClass.getName()));
		AnnotationAttributes manager = attributes.getAnnotation("manager");
		this.setMaxActiveSessions(manager.getNumber("maxActiveSessions").intValue());
		this.setMarshaller(manager.getEnum("marshaller"));
		this.setGranularity(manager.getEnum("granularity"));
		this.accept(attributes);
	}
}
