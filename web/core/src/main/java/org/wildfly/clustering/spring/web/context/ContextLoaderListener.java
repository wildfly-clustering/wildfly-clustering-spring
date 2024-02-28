/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.context;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * A {@link org.springframework.web.context.ContextLoaderListener} that configures a Spring web application context via XML or annotated components.
 * @author Paul Ferraro
 */
public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener {

	public ContextLoaderListener() {
		super(new XmlWebApplicationContext());
	}

	public ContextLoaderListener(Class<?>... componentClasses) {
		super(createWebApplicationContext(componentClasses));
	}

	private static WebApplicationContext createWebApplicationContext(Class<?>... componentClasses) {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(componentClasses);
		return context;
	}
}
