/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.clustering.spring.session.AbstractSmokeITCase;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;

/**
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class BeanSmokeITCase extends AbstractSmokeITCase {

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
		return deployment(BeanSmokeITCase.class)
				.setWebXML(AbstractSmokeITCase.class.getPackage(), "web.xml")
				.addAsWebInfResource(BeanSmokeITCase.class.getPackage(), "applicationContext.xml", "applicationContext.xml")
				.addAsWebInfResource(BeanSmokeITCase.class.getPackage(), "infinispan.xml", "infinispan.xml")
				;
	}

    @ArquillianResource(SessionServlet.class)
    @OperateOnDeployment(DEPLOYMENT_1)
    private URL baseURL1;

    @ArquillianResource(SessionServlet.class)
    @OperateOnDeployment(DEPLOYMENT_2)
    private URL baseURL2;

    @Test
    @RunAsClient
	public void test() throws Exception {
		this.accept(this.baseURL1, this.baseURL2);
	}
}
