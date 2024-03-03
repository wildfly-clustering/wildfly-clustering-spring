/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web;

import java.net.URI;
import java.util.function.BiConsumer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.wildfly.clustering.session.container.ClientTester;
import org.wildfly.clustering.session.container.SessionManagementEndpointConfiguration;
import org.wildfly.clustering.session.container.SessionManagementTester;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;

/**
 * @author Paul Ferraro
 */
public class AbstractSmokeITCase implements BiConsumer<URI, URI>, SessionManagementTesterConfiguration {
	protected static final String CONTAINER_1 = "tomcat-1";
	protected static final String CONTAINER_2 = "tomcat-2";
	protected static final String DEPLOYMENT_1 = "deployment-1";
	protected static final String DEPLOYMENT_2 = "deployment-2";

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
				.addClasses(PropertiesAsset.class, SessionManagementEndpointConfiguration.class)
				;
	}

	private final ClientTester tester = new SessionManagementTester(this);

	@AfterEach
	public void destroy() {
		this.tester.close();
	}

	@Override
	public void accept(URI baseURI1, URI baseURI2) {
		this.tester.test(baseURI1, baseURI2);
	}
}
