/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.infinispan.embedded;

import java.net.URI;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.clustering.spring.web.infinispan.embedded.context.ConfigContextLoaderListener;
import org.wildfly.clustering.spring.web.servlet.DispatcherServlet;

/**
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class InfinispanWebAnnotationSmokeITCase extends AbstractInfinispanWebSmokeITCase {

	@Deployment(name = DEPLOYMENT_1, testable = false)
	@TargetsContainer(CONTAINER_1)
	public static Archive<?> deployment1() {
		return deployment();
	}

	@Deployment(name = DEPLOYMENT_2, testable = false)
	@TargetsContainer(CONTAINER_2)
	public static Archive<?> deployment2() {
		return deployment();
	}

	private static Archive<?> deployment() {
		return AbstractInfinispanWebSmokeITCase.deployment(InfinispanWebAnnotationSmokeITCase.class)
				.addPackage(ConfigContextLoaderListener.class.getPackage())
				;
	}

	@ArquillianResource(DispatcherServlet.class)
	@OperateOnDeployment(DEPLOYMENT_1)
	private URI baseURI1;

	@ArquillianResource(DispatcherServlet.class)
	@OperateOnDeployment(DEPLOYMENT_2)
	private URI baseURI2;

	@Test
	@RunAsClient
	public void test() throws Exception {
		this.accept(this.baseURI1, this.baseURI2);
	}
}
