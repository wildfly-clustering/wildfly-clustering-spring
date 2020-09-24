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

package org.wildfly.clustering.web.spring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.infinispan.server.Server;
import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.junit4.InfinispanServerRule;
import org.infinispan.server.test.junit4.InfinispanServerRuleBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.wildfly.clustering.web.spring.servlet.SessionHandler;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSmokeITCase {
    public static final String CONTAINER_1 = "tomcat-1";
    public static final String CONTAINER_2 = "tomcat-2";
    public static final String DEPLOYMENT_1 = "deployment-1";
    public static final String DEPLOYMENT_2 = "deployment-2";

    static {
        System.setProperty(Server.INFINISPAN_CLUSTER_STACK, Server.DEFAULT_CLUSTER_STACK);
        System.setProperty(Server.INFINISPAN_CLUSTER_NAME, Server.DEFAULT_CLUSTER_NAME);
        System.setProperty(Server.INFINISPAN_NODE_NAME, InetAddress.getLoopbackAddress().getHostName());
    }

    @ClassRule
    public static final InfinispanServerRule SERVERS = InfinispanServerRuleBuilder.config("config.xml")
            .runMode(ServerRunMode.FORKED)
            .numServers(1)
            .build();

    public static Archive<?> deployment(Class<? extends AbstractSmokeITCase> testClass, Class<? extends SessionHandler> servletClass) {
        return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
                .addClass(SessionHandler.class)
                .addClasses(LoggingSessionListener.class, LoggingSessionIdentifierListener.class, LoggingSessionAttributeListener.class)
                .addClass(servletClass)
                .addAsWebInfResource(testClass.getPackage(), "applicationContext.xml", "applicationContext.xml")
                .setWebXML(AbstractSmokeITCase.class.getPackage(), "web.xml")
                ;
    }

    protected void test(URL baseURL1, URL baseURL2) throws IOException, URISyntaxException {
        URI uri1 = SessionHandler.createURI(baseURL1);
        URI uri2 = SessionHandler.createURI(baseURL2);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String sessionId = null;
            int value = 0;
            for (int i = 0; i < 5; i++) {
                for (URI uri : Arrays.asList(uri1, uri2)) {
                    try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                        Assert.assertEquals(String.valueOf(value++), response.getFirstHeader(SessionHandler.VALUE).getValue());
                        if (sessionId == null) {
                            sessionId = response.getFirstHeader(SessionHandler.SESSION_ID).getValue();
                        } else {
                            Assert.assertEquals(sessionId, response.getFirstHeader(SessionHandler.SESSION_ID).getValue());
                        }
                    }
                }
            }
            try (CloseableHttpResponse response = client.execute(new HttpPost(uri2))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                String newSessionId = response.getFirstHeader(SessionHandler.SESSION_ID).getValue();
                Assert.assertNotEquals(sessionId, newSessionId);
                sessionId = newSessionId;
            }
            try (CloseableHttpResponse response = client.execute(new HttpDelete(uri1))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId, response.getFirstHeader(SessionHandler.SESSION_ID).getValue());
            }
            try (CloseableHttpResponse response = client.execute(new HttpHead(uri2))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionHandler.SESSION_ID));
            }
        }
    }
}
