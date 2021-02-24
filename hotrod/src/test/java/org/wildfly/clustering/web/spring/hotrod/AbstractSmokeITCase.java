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

package org.wildfly.clustering.web.spring.hotrod;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.function.BiFunction;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.core.TestSystemPropertyNames;
import org.infinispan.server.test.junit4.InfinispanServerRuleBuilder;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.wildfly.clustering.web.spring.servlet.ServletHandler;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSmokeITCase implements ExceptionBiConsumer<URL, URL, Exception> {

    private static final String INFINISPAN_SERVER_HOME = System.getProperty("infinispan.server.home");

    @ClassRule
    public static final TestRule SERVERS = InfinispanServerRuleBuilder.config(INFINISPAN_SERVER_HOME + "/server/conf/infinispan.xml")
                .property(TestSystemPropertyNames.INFINISPAN_SERVER_HOME, INFINISPAN_SERVER_HOME)
                .runMode(ServerRunMode.FORKED)
                .numServers(1)
                .build();

    private static CloseableHttpClient createClient(URL url1, URL url2) {
        return HttpClients.createDefault();
    }

    private final BiFunction<URL, URL, CloseableHttpClient> provider;

    AbstractSmokeITCase() {
        this(AbstractSmokeITCase::createClient);
    }

    AbstractSmokeITCase(BiFunction<URL, URL, CloseableHttpClient> provider) {
        this.provider = provider;
    }

    @Override
    public void accept(URL baseURL1, URL baseURL2) throws Exception {
        URI uri1 = ServletHandler.createURI(baseURL1);
        URI uri2 = ServletHandler.createURI(baseURL2);

        try (CloseableHttpClient client = this.provider.apply(baseURL1, baseURL2)) {
            String sessionId = null;
            int value = 0;
            for (int i = 0; i < 4; i++) {
                for (URI uri : Arrays.asList(uri1, uri2)) {
                    for (int j = 0; j < 4; j++) {
                        try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                            Assert.assertEquals(String.valueOf(value++), response.getFirstHeader(ServletHandler.VALUE).getValue());
                            String requestSessionId = response.getFirstHeader(ServletHandler.SESSION_ID).getValue();
                            if (sessionId == null) {
                                sessionId = requestSessionId;
                            } else {
                                Assert.assertEquals(sessionId, requestSessionId);
                            }
                        }
                    }
                    // Grace time between failover requests
                    Thread.sleep(500);
                }
            }
            try (CloseableHttpResponse response = client.execute(new HttpDelete(uri1))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId, response.getFirstHeader(ServletHandler.SESSION_ID).getValue());
            }
            try (CloseableHttpResponse response = client.execute(new HttpHead(uri2))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                String newSessionId = response.containsHeader(ServletHandler.SESSION_ID) ? response.getFirstHeader(ServletHandler.SESSION_ID).getValue() : null;
                Assert.assertNotEquals(sessionId, newSessionId);
            }
        }
    }
}
