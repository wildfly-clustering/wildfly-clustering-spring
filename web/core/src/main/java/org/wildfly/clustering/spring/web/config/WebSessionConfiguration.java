/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.config;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.function.Consumer;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.spring.context.config.SessionManagementConfiguration;
import org.wildfly.clustering.spring.web.DistributableWebSessionManager;
import org.wildfly.clustering.spring.web.DistributableWebSessionManagerConfiguration;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public abstract class WebSessionConfiguration extends SessionManagementConfiguration<ServletContext> implements ServletContextAware {

	private WebSessionIdResolver resolver = new CookieWebSessionIdResolver();

	private ServletContext context;

	protected WebSessionConfiguration(Class<? extends Annotation> annotationClass) {
		super(annotationClass);
	}

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

	@Autowired(required = false)
	public void setSessionIdentifierResolver(WebSessionIdResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public String getServerName() {
		return this.getContext().getVirtualServerName();
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
}
