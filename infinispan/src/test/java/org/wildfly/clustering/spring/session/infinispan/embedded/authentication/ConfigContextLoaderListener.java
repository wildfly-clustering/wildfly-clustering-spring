/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.authentication;

import jakarta.servlet.annotation.WebListener;

/**
 * Custom servlet context listener that uses annotation-based registration, using our Config.
 * @author Paul Ferraro
 */
@WebListener
public class ConfigContextLoaderListener extends org.wildfly.clustering.spring.session.context.ContextLoaderListener {

	public ConfigContextLoaderListener() {
		super(SecurityConfig.class, Config.class);
	}
}
