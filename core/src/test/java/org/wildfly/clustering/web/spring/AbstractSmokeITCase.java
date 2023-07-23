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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
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
			AtomicReference<String> sessionId = new AtomicReference<>();
			AtomicInteger value = new AtomicInteger(0);
			for (int i = 0; i < 4; i++) {
				for (URI uri : Arrays.asList(uri1, uri2)) {
					for (int j = 0; j < 4; j++) {
						Map.Entry<String, String> result = client.execute(new HttpGet(uri), response -> {
							Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
							return Map.entry(response.getFirstHeader(SessionServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionServlet.VALUE).getValue());
						});
						Assert.assertEquals(String.valueOf(value.getAndIncrement()), result.getValue());
						String requestSessionId = result.getKey();
						if (!sessionId.compareAndSet(null, requestSessionId)) {
							Assert.assertEquals(sessionId.get(), requestSessionId);
						}
					}
					if (!this.transactional) {
						// Grace time between failover requests
						Thread.sleep(500);
					}
				}
			}
			String requestSessionId = client.execute(new HttpDelete(uri1), response -> {
				Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
				return response.getFirstHeader(SessionServlet.SESSION_ID).getValue();
			});
			Assert.assertEquals(sessionId.get(), requestSessionId);
			requestSessionId = client.execute(new HttpHead(uri2), response -> {
				Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
				return Optional.ofNullable(response.getFirstHeader(SessionServlet.SESSION_ID)).map(Header::getValue).orElse(null);
			});
			Assert.assertNotEquals(sessionId.get(), requestSessionId);
		}
	}
}
