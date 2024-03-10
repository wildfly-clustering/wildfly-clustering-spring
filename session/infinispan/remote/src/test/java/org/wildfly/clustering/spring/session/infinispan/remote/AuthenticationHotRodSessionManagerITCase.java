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
import java.net.http.HttpClient;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.session.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.infinispan.remote.authentication.Config;

/**
 * @author Paul Ferraro
 */
public class AuthenticationHotRodSessionManagerITCase extends AbstractHotRodSessionManagerITCase {

	@Test
	public void test() {
		WebArchive archive = this.get()
				.addAsWebInfResource(new PropertiesAsset(this.apply(new Properties())), "classes/application.properties")
				.addPackage(Config.class.getPackage())
				.addPackage(SecurityInitializer.class.getPackage())
				;

		// URI uri1 = this.baseURI1.resolve(SessionManagementEndpointConfiguration.ENDPOINT_NAME);
		// Verify that authentication is required
		// HttpResponse<Void> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri1).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding());
		// Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.statusCode());

		this.accept(archive);
	}

	@Override
	public UnaryOperator<HttpClient.Builder> getHttpClientConfigurator() {
		return builder -> builder.authenticator(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("admin", "password".toCharArray());
			}
		});
	}
}
