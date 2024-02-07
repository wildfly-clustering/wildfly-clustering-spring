/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import static org.junit.jupiter.api.Assertions.*;

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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.spring.session.servlet.SessionServlet;
import org.wildfly.clustering.spring.session.servlet.context.HttpSessionApplicationInitializer;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * @author Paul Ferraro
 */
public class AbstractSmokeITCase implements ExceptionBiConsumer<URL, URL, Exception> {
	protected static final String CONTAINER_1 = "tomcat-1";
	protected static final String CONTAINER_2 = "tomcat-2";
	protected static final String DEPLOYMENT_1 = "deployment-1";
	protected static final String DEPLOYMENT_2 = "deployment-2";

	protected static WebArchive createWebArchive(Class<? extends AbstractSmokeITCase> testClass) {
		return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
				.addPackage(SessionServlet.class.getPackage())
				.addPackage(HttpSessionApplicationInitializer.class.getPackage())
				.setWebXML(AbstractSmokeITCase.class.getPackage(), "web.xml")
				;
	}

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
							assertEquals(HttpStatus.SC_OK, response.getCode());
							return Map.entry(response.getFirstHeader(SessionServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionServlet.VALUE).getValue());
						});
						assertEquals(String.valueOf(value.getAndIncrement()), result.getValue());
						String requestSessionId = result.getKey();
						if (!sessionId.compareAndSet(null, requestSessionId)) {
							assertEquals(sessionId.get(), requestSessionId);
						}
					}
					if (!this.transactional) {
						// Grace time between failover requests
						Thread.sleep(500);
					}
				}
			}
			String requestSessionId = client.execute(new HttpDelete(uri1), response -> {
				assertEquals(HttpStatus.SC_OK, response.getCode());
				return response.getFirstHeader(SessionServlet.SESSION_ID).getValue();
			});
			assertEquals(sessionId.get(), requestSessionId);
			requestSessionId = client.execute(new HttpHead(uri2), response -> {
				assertEquals(HttpStatus.SC_OK, response.getCode());
				return Optional.ofNullable(response.getFirstHeader(SessionServlet.SESSION_ID)).map(Header::getValue).orElse(null);
			});
			assertNotEquals(sessionId.get(), requestSessionId);
		}
	}
}
