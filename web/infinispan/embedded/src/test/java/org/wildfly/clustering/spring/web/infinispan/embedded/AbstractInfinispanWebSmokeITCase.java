/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.embedded;

import java.net.http.HttpClient;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.spring.web.AbstractSmokeITCase;
import org.wildfly.clustering.spring.web.AbstractWebSmokeITCase;

/**
 * @author Paul Ferraro
 */
public class AbstractInfinispanWebSmokeITCase extends AbstractWebSmokeITCase {

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return AbstractWebSmokeITCase.deployment(testClass)
				.addAsWebInfResource(AbstractInfinispanWebSmokeITCase.class.getPackage(), "infinispan.xml", "infinispan.xml")
				;
	}

	protected AbstractInfinispanWebSmokeITCase() {
		super(HttpClient.newBuilder());
	}
}
