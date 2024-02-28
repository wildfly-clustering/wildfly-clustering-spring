/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.context;

import jakarta.servlet.annotation.WebListener;

import org.wildfly.clustering.spring.web.context.ContextLoaderListener;

/**
 * Custom servlet context listener that uses annotation-based registration, using our Config.
 * @author Paul Ferraro
 */
@WebListener
public class ConfigContextLoaderListener extends ContextLoaderListener {

	public ConfigContextLoaderListener() {
		super(Config.class);
	}
}
