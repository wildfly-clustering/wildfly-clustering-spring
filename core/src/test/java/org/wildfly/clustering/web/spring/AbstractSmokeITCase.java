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

package org.wildfly.clustering.web.spring;

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
import org.junit.Assert;
import org.wildfly.clustering.web.spring.servlet.SessionServlet;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * @author Paul Ferraro
 */
public class AbstractSmokeITCase implements ExceptionBiConsumer<URL, URL, Exception> {
    protected static final String CONTAINER_1 = "tomcat-1";
    protected static final String CONTAINER_2 = "tomcat-2";
    protected static final String DEPLOYMENT_1 = "deployment-1";
    protected static final String DEPLOYMENT_2 = "deployment-2";

    private static CloseableHttpClient createClient(URL url1, URL url2) {
        return HttpClients.createDefault();
    }

    private final boolean transactional;
    private final BiFunction<URL, URL, CloseableHttpClient> provider;

    protected AbstractSmokeITCase() {
        this(false);
    }

    protected AbstractSmokeITCase(BiFunction<URL, URL, CloseableHttpClient> provider) {
        this(false, provider);
    }

    protected AbstractSmokeITCase(boolean transactional) {
        this(transactional, AbstractSmokeITCase::createClient);
    }

    protected AbstractSmokeITCase(boolean transactional, BiFunction<URL, URL, CloseableHttpClient> provider) {
        this.transactional = transactional;
        this.provider = provider;
    }

    @Override
    public void accept(URL baseURL1, URL baseURL2) throws Exception {
        URI uri1 = SessionServlet.createURI(baseURL1);
        URI uri2 = SessionServlet.createURI(baseURL2);

        try (CloseableHttpClient client = this.provider.apply(baseURL1, baseURL2)) {
            String sessionId = null;
            int value = 0;
            for (int i = 0; i < 4; i++) {
                for (URI uri : Arrays.asList(uri1, uri2)) {
                    for (int j = 0; j < 4; j++) {
                        try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                            Assert.assertEquals(String.valueOf(value++), response.getFirstHeader(SessionServlet.VALUE).getValue());
                            String requestSessionId = response.getFirstHeader(SessionServlet.SESSION_ID).getValue();
                            if (sessionId == null) {
                                sessionId = requestSessionId;
                            } else {
                                Assert.assertEquals(sessionId, requestSessionId);
                            }
                        }
                    }
                    if (!this.transactional) {
                        // Grace time between failover requests
                        Thread.sleep(500);
                    }
                }
            }
            try (CloseableHttpResponse response = client.execute(new HttpDelete(uri1))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId, response.getFirstHeader(SessionServlet.SESSION_ID).getValue());
            }
            try (CloseableHttpResponse response = client.execute(new HttpHead(uri2))) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                String newSessionId = response.containsHeader(SessionServlet.SESSION_ID) ? response.getFirstHeader(SessionServlet.SESSION_ID).getValue() : null;
                Assert.assertNotEquals(sessionId, newSessionId);
            }
        }
    }
}
