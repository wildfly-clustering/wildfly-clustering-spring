/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.servlet;

import java.io.IOException;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;

import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Decorates a {@link ServletHttpHandlerAdapter} servlet to workaround the inability of Arquillian to detect programmatically registered servlets.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = "/", asyncSupported = true, loadOnStartup = 1)
public class DispatcherServlet implements Servlet {

	static final String SERVLET_PATH = "/";

	private Servlet servlet;

	@Override
	public void init(ServletConfig config) throws ServletException {
		ApplicationContext context = (ApplicationContext) config.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context).build();
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
}
