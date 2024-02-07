/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.URL;

import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.spring.session.infinispan.embedded.context.ConfigContextLoaderListener;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;
import org.wildfly.clustering.spring.session.servlet.TestSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public class AnnotationSmokeITCase extends AbstractHotRodSmokeITCase {

	@RegisterExtension
	static final ArquillianExtension ARQUILLIAN = new ArquillianExtension();

	@Deployment(name = DEPLOYMENT_1, testable = false)
	@TargetsContainer(CONTAINER_1)
	static Archive<?> deployment1() {
		return deployment();
	}

	@Deployment(name = DEPLOYMENT_2, testable = false)
	@TargetsContainer(CONTAINER_2)
	static Archive<?> deployment2() {
		return deployment();
	}

	private static Archive<?> deployment() {
		return deployment(AnnotationSmokeITCase.class)
				.addPackage(ConfigContextLoaderListener.class.getPackage())
				.addAsServiceProvider(SerializationContextInitializer.class.getName(), TestSerializationContextInitializer.class.getName() + "Impl")
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
