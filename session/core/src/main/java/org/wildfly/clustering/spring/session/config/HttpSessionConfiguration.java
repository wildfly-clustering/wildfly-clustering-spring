/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.IndexResolver;
import org.springframework.session.PrincipalNameIndexResolver;
import org.springframework.session.Session;
import org.springframework.web.context.ServletContextAware;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.spec.servlet.HttpSessionProvider;
import org.wildfly.clustering.spring.context.config.SessionManagementConfiguration;
import org.wildfly.clustering.spring.security.SpringSecurityImmutability;
import org.wildfly.clustering.spring.session.DistributableSessionRepository;
import org.wildfly.clustering.spring.session.DistributableSessionRepositoryConfiguration;
import org.wildfly.clustering.spring.session.ImmutableSessionDestroyAction;
import org.wildfly.clustering.spring.session.MutableIndexingConfiguration;
import org.wildfly.clustering.spring.session.SpringSession;
import org.wildfly.clustering.spring.session.UserConfiguration;
import org.wildfly.clustering.spring.web.util.SpringWebImmutability;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public abstract class HttpSessionConfiguration extends SessionManagementConfiguration<ServletContext> implements ApplicationEventPublisherAware, ServletContextAware, MutableIndexingConfiguration {

	public static final String DEFAULT_SPRING_SECURITY_INDEX_ID = "SPRING_SECURITY_CONTEXT";
	public static final String DEFAULT_STRING_SECURITY_INDEX_NAME = "org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME";
	public static final Map<String, String> DEFAULT_SPRING_SECURITY_INDEXES = Map.of(DEFAULT_SPRING_SECURITY_INDEX_ID, DEFAULT_STRING_SECURITY_INDEX_NAME);
	public static final IndexResolver<Session> DEFAULT_SPRING_SECURITY_INDEX_RESOLVER = new PrincipalNameIndexResolver<>();

	private ServletContext context;
	private ApplicationEventPublisher publisher;
	private Map<String, String> indexes;
	private IndexResolver<Session> indexResolver;

	protected HttpSessionConfiguration(Class<? extends Annotation> annotationClass, Map<String, String> defaultIndexes, IndexResolver<Session> defaultIndexResolver) {
		super(annotationClass);
		this.indexes = defaultIndexes;
		this.indexResolver = defaultIndexResolver;
	}

	@Bean
	public FindByIndexNameSessionRepository<SpringSession> sessionRepository(SessionManager<Void> manager, UserConfiguration userConfiguration) {
		BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> sessionDestroyAction = new ImmutableSessionDestroyAction<>(this.publisher, this.getContext(), HttpSessionProvider.INSTANCE, userConfiguration);
		DistributableSessionRepositoryConfiguration configuration = new DistributableSessionRepositoryConfiguration() {
			@Override
			public SessionManager<Void> getSessionManager() {
				return manager;
			}

			@Override
			public ApplicationEventPublisher getEventPublisher() {
				return HttpSessionConfiguration.this.publisher;
			}

			@Override
			public BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> getSessionDestroyAction() {
				return sessionDestroyAction;
			}

			@Override
			public UserConfiguration getUserConfiguration() {
				return userConfiguration;
			}
		};
		return new DistributableSessionRepository(configuration);
	}

	@Override
	public void setServletContext(ServletContext context) {
		this.context = context;
	}

	@Override
	public void setIndexes(Map<String, String> indexes) {
		this.indexes = indexes;
	}

	@Override
	public void setIndexResolver(IndexResolver<Session> resolver) {
		this.indexResolver = resolver;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public String getDeploymentName() {
		return this.context.getVirtualServerName() + this.context.getContextPath();
	}

	@Override
	public Map<String, String> getIndexes() {
		return this.indexes;
	}

	@Override
	public IndexResolver<Session> getIndexResolver() {
		return this.indexResolver;
	}

	@Override
	public String getServerName() {
		return this.getContext().getVirtualServerName();
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.context.getClassLoader();
	}

	@Override
	public ServletContext getContext() {
		return this.context;
	}

	@Override
	public Consumer<ImmutableSession> getExpirationListener() {
		return Functions.discardingConsumer();
	}

	@Override
	public Duration getTimeout() {
		return Duration.ofMinutes(this.getContext().getSessionTimeout());
	}

	@Override
	public Immutability getImmutability() {
		List<Immutability> loadedImmutabilities = new LinkedList<>();
		for (Immutability loadedImmutability : ServiceLoader.load(Immutability.class, this.context.getClassLoader())) {
			loadedImmutabilities.add(loadedImmutability);
		}
		return Immutability.composite(Stream.concat(Stream.of(Immutability.getDefault(), SpringSecurityImmutability.INSTANCE, SpringWebImmutability.MUTEX), loadedImmutabilities.stream()).toList());
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		if (attributes.containsKey("indexing")) {
			AnnotationAttributes indexing = attributes.getAnnotation("indexing");
			Map<String, String> indexes = new TreeMap<>();
			for (AnnotationAttributes index : indexing.getAnnotationArray("indexes")) {
				indexes.put(index.getString("id"), index.getString("name"));
			}
			this.setIndexes(indexes);
			Class<? extends IndexResolver<Session>> resolverClass = indexing.getClass("resolverClass");
			try {
				this.setIndexResolver(resolverClass.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new IllegalArgumentException(resolverClass.getCanonicalName());
			}
		}
	}
}
