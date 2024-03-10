/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.context;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * The servlet filter otherwise registered by {@link AbstractHttpSessionApplicationInitializer}.
 * @author Paul Ferraro
 */
@WebFilter(filterName = AbstractHttpSessionApplicationInitializer.DEFAULT_FILTER_NAME, urlPatterns = "/*", dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.ERROR })
public class SpringSessionFilter extends DelegatingFilterProxy {

	public SpringSessionFilter() {
		super(AbstractHttpSessionApplicationInitializer.DEFAULT_FILTER_NAME);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		try {
			super.doFilter(request, response, filterChain);
		} catch (ServletException | IOException | RuntimeException | Error e) {
			e.printStackTrace(System.err);
			throw e;
		}
	}
}
