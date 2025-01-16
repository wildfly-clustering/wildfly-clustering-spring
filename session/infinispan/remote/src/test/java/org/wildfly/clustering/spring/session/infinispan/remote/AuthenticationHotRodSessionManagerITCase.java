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
import java.util.Properties;
import java.util.function.UnaryOperator;

import jakarta.servlet.http.HttpServletResponse;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.arquillian.Deployment;
import org.wildfly.clustering.session.container.SessionManagementTester;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.session.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.infinispan.remote.authentication.Config;

/**
 * @author Paul Ferraro
 */
public class AuthenticationHotRodSessionManagerITCase extends AbstractHotRodSessionManagerITCase {

	public AuthenticationHotRodSessionManagerITCase() {
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

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration)
				.addAsWebInfResource(new PropertiesAsset(this.apply(new Properties())), "classes/application.properties")
				.addPackage(Config.class.getPackage())
				.addPackage(SecurityInitializer.class.getPackage())
				;
	}

	@Test
	public void test() {
		this.run();
	}
}
