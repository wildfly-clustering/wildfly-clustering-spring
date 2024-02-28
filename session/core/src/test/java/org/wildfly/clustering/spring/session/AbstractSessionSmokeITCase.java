/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session;

import java.net.http.HttpClient;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.spring.session.context.SpringSessionFilter;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;
import org.wildfly.clustering.spring.web.AbstractSmokeITCase;

/**
 * @author Paul Ferraro
 */
public class AbstractSessionSmokeITCase extends AbstractSmokeITCase {

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return AbstractSmokeITCase.deployment(testClass)
				.addPackage(SessionServlet.class.getPackage())
				.addPackage(SpringSessionFilter.class.getPackage())
				;
	}

	protected AbstractSessionSmokeITCase(HttpClient.Builder builder) {
		super(builder);
	}
}
