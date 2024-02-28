/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.http.HttpClient;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.spring.session.AbstractSessionSmokeITCase;
import org.wildfly.clustering.spring.web.AbstractSmokeITCase;

/**
 * @author Paul Ferraro
 */
public class AbstractInfinispanSessionSmokeITCase extends AbstractSessionSmokeITCase {

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return AbstractSessionSmokeITCase.deployment(testClass)
				.addAsWebInfResource(AbstractInfinispanSessionSmokeITCase.class.getPackage(), "infinispan.xml", "infinispan.xml")
				;
	}

	protected AbstractInfinispanSessionSmokeITCase() {
		this(HttpClient.newBuilder());
	}

	protected AbstractInfinispanSessionSmokeITCase(HttpClient.Builder builder) {
		super(builder);
	}
}
