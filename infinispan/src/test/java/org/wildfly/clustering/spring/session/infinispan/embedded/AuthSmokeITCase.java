/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.URI;
import java.net.URL;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.infinispan.protostream.SerializationContextInitializer;
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
import org.wildfly.clustering.spring.security.authentication.SecurityInitializer;
import org.wildfly.clustering.spring.session.AbstractSmokeITCase;
import org.wildfly.clustering.spring.session.infinispan.embedded.authentication.ConfigContextLoaderListener;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;
import org.wildfly.clustering.spring.session.servlet.TestSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class AuthSmokeITCase extends AbstractSmokeITCase {

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
		return deployment(AuthSmokeITCase.class)
				.addPackage(ConfigContextLoaderListener.class.getPackage())
				.addPackage(SecurityInitializer.class.getPackage())
				.addAsWebInfResource(AuthSmokeITCase.class.getPackage(), "infinispan.xml", "infinispan.xml")
				.addAsServiceProvider(SerializationContextInitializer.class.getName(), TestSerializationContextInitializer.class.getName() + "Impl")
				;
	}

	public AuthSmokeITCase() {
		super(AuthSmokeITCase::createClient);
	}

	private static CloseableHttpClient createClient(URL url1, URL url2) {
		CredentialsStore provider = new BasicCredentialsProvider();
		Credentials credentials = new UsernamePasswordCredentials("admin", "password".toCharArray());
		provider.setCredentials(new AuthScope(url1.getHost(), url1.getPort()), credentials);
		provider.setCredentials(new AuthScope(url2.getHost(), url2.getPort()), credentials);
		return HttpClients.custom().setDefaultCredentialsProvider(provider).build();
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
		URI uri1 = SessionServlet.createURI(this.baseURL1);
		// Verify that authentication is required
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			client.execute(new HttpHead(uri1), response -> {
				Assertions.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode());
				return null;
			});
		}
		this.accept(this.baseURL1, this.baseURL2);
	}
}
