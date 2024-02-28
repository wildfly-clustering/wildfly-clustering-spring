/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.spring.session.infinispan.remote;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import jakarta.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.spring.session.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.infinispan.remote.authentication.ConfigContextLoaderListener;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;
import org.wildfly.clustering.spring.web.SmokeITParameters;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionAuthSmokeITCase extends AbstractHotRodSessionSmokeITCase {

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
		return deployment(HotRodSessionAuthSmokeITCase.class)
				.addPackage(SecurityInitializer.class.getPackage())
				.addPackage(ConfigContextLoaderListener.class.getPackage())
				;
	}

	public HotRodSessionAuthSmokeITCase() {
		super(HttpClient.newBuilder().authenticator(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("admin", "password".toCharArray());
			}
		}));
	}

	@ArquillianResource(SessionServlet.class)
	@OperateOnDeployment(DEPLOYMENT_1)
	private URI baseURI1;

	@ArquillianResource(SessionServlet.class)
	@OperateOnDeployment(DEPLOYMENT_2)
	private URI baseURI2;

	@Test
	@RunAsClient
	public void test() throws Exception {
		URI uri1 = this.baseURI1.resolve(SmokeITParameters.ENDPOINT_NAME);
		// Verify that authentication is required
		HttpResponse<Void> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri1).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding());
		Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.statusCode());
		this.accept(this.baseURI1, this.baseURI2);
	}
}
