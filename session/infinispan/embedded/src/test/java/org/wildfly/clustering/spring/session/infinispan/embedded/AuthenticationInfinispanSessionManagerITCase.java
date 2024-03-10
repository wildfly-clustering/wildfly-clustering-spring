/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.util.function.UnaryOperator;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.spring.session.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.infinispan.embedded.authentication.Config;

/**
 * @author Paul Ferraro
 */
public class AuthenticationInfinispanSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

	@Test
	public void test() {
		WebArchive archive = this.get()
				.addPackage(Config.class.getPackage())
				.addPackage(SecurityInitializer.class.getPackage())
				;
/*
		URI uri1 = this.baseURI1.resolve(SessionManagementEndpointConfiguration.ENDPOINT_NAME);
		// Verify that authentication is required
		HttpResponse<Void> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri1).build(), BodyHandlers.discarding());
		Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.statusCode());
*/
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
