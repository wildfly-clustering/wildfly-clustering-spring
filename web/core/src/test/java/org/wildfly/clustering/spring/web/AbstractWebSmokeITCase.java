/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.net.http.HttpClient;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.spring.web.context.SessionHandler;
import org.wildfly.clustering.spring.web.servlet.DispatcherServlet;

/**
 * @author Paul Ferraro
 */
public class AbstractWebSmokeITCase extends AbstractSmokeITCase {

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return AbstractSmokeITCase.deployment(testClass)
				.addPackage(SessionHandler.class.getPackage())
				.addPackage(DispatcherServlet.class.getPackage())
				;
	}

	protected AbstractWebSmokeITCase(HttpClient.Builder builder) {
		super(builder);
	}
}
