/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.config;

import java.lang.annotation.Annotation;
import java.time.Duration;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.spring.context.config.SessionManagementConfiguration;
import org.wildfly.clustering.spring.web.DistributableWebSessionManager;
import org.wildfly.clustering.spring.web.DistributableWebSessionManagerConfiguration;

/**
 * A Spring bean that configures and produces a Spring Web session manager.
 * @author Paul Ferraro
 */
public abstract class WebSessionManagerConfiguration extends SessionManagementConfiguration<ServletContext> implements ServletContextAware {

	private WebSessionIdResolver resolver = new CookieWebSessionIdResolver();

	private ServletContext context;

	/**
	 * Creates a session manager configuration bean.
	 * @param annotationClass the configuration annotation class
	 */
	protected WebSessionManagerConfiguration(Class<? extends Annotation> annotationClass) {
		super(annotationClass);
	}

	/**
	 * Returns the Spring Web session manager produced by this bean.
	 * @param manager a distributed session manager
	 * @return the Spring Web session manager produced by this bean.
	 */
	@Bean(WebHttpHandlerBuilder.WEB_SESSION_MANAGER_BEAN_NAME)
	public WebSessionManager webSessionManager(SessionManager<Void> manager) {
		WebSessionIdResolver resolver = this.resolver;
		DistributableWebSessionManagerConfiguration configuration = new DistributableWebSessionManagerConfiguration() {
			@Override
			public SessionManager<Void> getSessionManager() {
				return manager;
			}

			@Override
			public WebSessionIdResolver getSessionIdentifierResolver() {
				return resolver;
			}
		};
		return new DistributableWebSessionManager(configuration);
	}

	@Override
	public void setServletContext(ServletContext context) {
		this.context = context;
	}

	@Override
	public String getDeploymentName() {
		return this.context.getVirtualServerName() + this.context.getContextPath();
	}

	/**
	 * Specifies the session identifier resolver.
	 * @param resolver a session identifier resolver.
	 */
	@Autowired(required = false)
	public void setSessionIdentifierResolver(WebSessionIdResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public String getServerName() {
		return this.context.getVirtualServerName();
	}

	@Override
	public ServletContext getContext() {
		return this.context;
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.context.getClassLoader();
	}

	@Override
	public Consumer<ImmutableSession> getExpirationListener() {
		return Consumer.empty();
	}

	@Override
	public Duration getTimeout() {
		return Duration.ofMinutes(this.getContext().getSessionTimeout());
	}
}
