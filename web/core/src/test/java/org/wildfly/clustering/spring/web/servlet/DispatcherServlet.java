/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;

import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import reactor.core.publisher.Mono;

/**
 * Decorates a {@link ServletHttpHandlerAdapter} servlet to workaround the inability of Arquillian to detect programmatically registered servlets.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = "/", asyncSupported = true, loadOnStartup = 1)
public class DispatcherServlet implements Servlet, UnaryOperator<HttpHandler> {

	static final String SERVLET_PATH = "/";

	private Servlet servlet;

	@Override
	public void init(ServletConfig config) throws ServletException {
		ApplicationContext context = (ApplicationContext) config.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context).httpHandlerDecorator(this).build();
		this.servlet = new ServletHttpHandlerAdapter(handler);
		this.servlet.init(config);
	}

	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		this.servlet.service(request, response);
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servlet.getServletConfig();
	}

	@Override
	public String getServletInfo() {
		return this.servlet.getServletInfo();
	}

	@Override
	public void destroy() {
		this.servlet.destroy();
	}

	@Override
	public HttpHandler apply(HttpHandler handler) {
		UnaryOperator<ServerHttpResponse> responseDecorator = new UnaryOperator<>() {
			@Override
			public ServerHttpResponse apply(ServerHttpResponse response) {
				return new ServerHttpResponseDecorator(response) {
					private final AtomicBoolean completed = new AtomicBoolean(false);

					@Override
					public Mono<Void> setComplete() {
						return this.completed.compareAndSet(false, true) ? response.setComplete() : Mono.empty();
					}
				};
			}
		};
		// HttpHeadResponseDecorator is buggy and triggers ServerHttpResponse.setComplete() twice.
		return new HttpHandler() {
			@Override
			public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
				return handler.handle(request, request.getMethod().matches("HEAD") ? responseDecorator.apply(response) : response);
			}
		};
	}
}
