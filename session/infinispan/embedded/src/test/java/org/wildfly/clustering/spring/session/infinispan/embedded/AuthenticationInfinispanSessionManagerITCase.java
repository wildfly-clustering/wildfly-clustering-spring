/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.servlet.http.HttpServletResponse;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.arquillian.Deployment;
import org.wildfly.clustering.session.container.SessionManagementTester;
import org.wildfly.clustering.spring.session.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.infinispan.embedded.authentication.Config;

/**
 * @author Paul Ferraro
 */
public class AuthenticationInfinispanSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

	public AuthenticationInfinispanSessionManagerITCase() {
		super(configuration -> new SessionManagementTester(configuration) {
			@Override
			public void accept(List<Deployment> deployments) {
				List<URI> endpoints = deployments.stream().map(configuration::locateEndpoint).toList();
				try {
					for (URI endpoint : endpoints) {
						// Verify that authentication is required
						HttpResponse<Void> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(endpoint).method(SessionManagementTester.HttpMethod.HEAD.name(), BodyPublishers.noBody()).build(), BodyHandlers.discarding());
						Assertions.assertThat(response.statusCode()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
					}
					super.accept(deployments);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					Assertions.fail(e);
				}
			}
		}, new SessionManagementTesterConfiguration() {
			@Override
			public UnaryOperator<HttpClient.Builder> getHttpClientConfigurator() {
				return builder -> builder.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("admin", "password".toCharArray());
					}
				});
			}
		});
	}

	@Test
	public void test() {
		this.run();
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration)
				.addPackage(Config.class.getPackage())
				.addPackage(SecurityInitializer.class.getPackage())
				;
	}
}
