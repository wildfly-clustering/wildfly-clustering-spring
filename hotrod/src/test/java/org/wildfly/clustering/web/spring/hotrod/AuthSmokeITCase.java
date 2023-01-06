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

package org.wildfly.clustering.web.spring.hotrod;

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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.clustering.web.spring.auth.SecurityInitializer;
import org.wildfly.clustering.web.spring.hotrod.auth.ConfigContextLoaderListener;
import org.wildfly.clustering.web.spring.servlet.SessionServlet;
import org.wildfly.clustering.web.spring.servlet.TestSerializationContextInitializer;
import org.wildfly.clustering.web.spring.servlet.context.HttpSessionApplicationInitializer;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
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
        return ShrinkWrap.create(WebArchive.class, AuthSmokeITCase.class.getSimpleName() + ".war")
                .addPackage(SessionServlet.class.getPackage())
                .addPackage(HttpSessionApplicationInitializer.class.getPackage())
                .addPackage(ConfigContextLoaderListener.class.getPackage())
                .addPackage(SecurityInitializer.class.getPackage())
                .addClass(HttpSessionApplicationInitializer.class)
                .addAsWebInfResource(org.wildfly.clustering.web.spring.AbstractSmokeITCase.class.getPackage(), "applicationContext.xml", "applicationContext.xml")
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

    @Test
    public void test(@ArquillianResource(SessionServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(SessionServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        URI uri1 = SessionServlet.createURI(baseURL1);
        // Verify that authentication is required
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            client.execute(new HttpHead(uri1), response -> {
                Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode());
                return null;
            });
        }
        this.accept(baseURL1, baseURL2);
    }
}
