/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.function.UnaryOperator;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.clustering.session.container.SessionManagementEndpointConfiguration;
import org.wildfly.clustering.spring.session.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.infinispan.embedded.authentication.ConfigContextLoaderListener;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;

/**
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class InfinispanSessionAuthSmokeITCase extends AbstractInfinispanSessionSmokeITCase {

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
		return deployment(InfinispanSessionAuthSmokeITCase.class)
				.addPackage(ConfigContextLoaderListener.class.getPackage())
				.addPackage(SecurityInitializer.class.getPackage())
				;
	}

	@Override
	public UnaryOperator<Builder> getHttpClientConfigurator() {
		return builder -> builder.authenticator(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("admin", "password".toCharArray());
			}
		});
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
		URI uri1 = this.baseURI1.resolve(SessionManagementEndpointConfiguration.ENDPOINT_NAME);
		// Verify that authentication is required
		HttpResponse<Void> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri1).build(), BodyHandlers.discarding());
		Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.statusCode());
		this.accept(this.baseURI1, this.baseURI2);
	}
}
