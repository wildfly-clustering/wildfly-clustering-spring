/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * @author Paul Ferraro
 */
public class AbstractSmokeITCase implements ExceptionBiConsumer<URI, URI, Exception> {
	protected static final String CONTAINER_1 = "tomcat-1";
	protected static final String CONTAINER_2 = "tomcat-2";
	protected static final String DEPLOYMENT_1 = "deployment-1";
	protected static final String DEPLOYMENT_2 = "deployment-2";

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
				.addClasses(PropertiesAsset.class, SmokeITParameters.class)
				;
	}

	private final boolean transactional;
	private final HttpClient client;

	protected AbstractSmokeITCase(HttpClient.Builder builder) {
		this(false, builder);
	}

	protected AbstractSmokeITCase(boolean transactional, HttpClient.Builder builder) {
		this.transactional = transactional;
		CookieManager manager = new CookieManager();
		manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		this.client = builder.cookieHandler(manager).build();
	}

	@Override
	public void accept(URI baseURI1, URI baseURI2) throws Exception {
		URI uri1 = baseURI1.resolve(SmokeITParameters.ENDPOINT_NAME);
		URI uri2 = baseURI2.resolve(SmokeITParameters.ENDPOINT_NAME);
		int iterations = 4;
		int concurrentRequests = 20;
		AtomicReference<String> sessionId = new AtomicReference<>();
		AtomicLong expected = new AtomicLong(0);
		for (int i = 0; i < iterations; i++) {
			for (URI uri : Arrays.asList(uri1, uri2)) {
				int count = i;
				long value = this.client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.discarding()).thenApply(response -> {
					assertEquals(HttpServletResponse.SC_OK, response.statusCode(), Integer.toString(count));
					String requestSessionId = response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none");
					// Validate propagation of session ID
					if (!sessionId.compareAndSet(null, requestSessionId)) {
						assertEquals(sessionId.get(), requestSessionId);
					}
					return response.headers().firstValueAsLong(SmokeITParameters.VALUE).orElse(0);
				}).join();
				Assertions.assertEquals(expected.getAndIncrement(), value);
				// Validate session propagation
				this.client.sendAsync(HttpRequest.newBuilder(uri).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding()).thenAccept(response -> {
					assertEquals(HttpServletResponse.SC_OK, response.statusCode());
					assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none"));
				}).join();
				// Perform a bunch of concurrent requests incrementing the session attribute
				List<CompletableFuture<Long>> futures = new ArrayList<>(concurrentRequests);
				for (int j = 0; j < concurrentRequests; j++) {
					futures.add(this.client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.discarding()).thenApply(response -> {
						assertEquals(HttpServletResponse.SC_OK, response.statusCode());
						assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none"));
						return response.headers().firstValueAsLong(SmokeITParameters.VALUE).orElse(0);
					}));
					expected.incrementAndGet();
				}
				for (CompletableFuture<Long> future : futures) {
					future.join();
				}
				// Verify expected session attribute value following concurrent updates
				value = this.client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.discarding()).thenApply(response -> {
					assertEquals(HttpServletResponse.SC_OK, response.statusCode());
					assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none"));
					return response.headers().firstValueAsLong(SmokeITParameters.VALUE).orElse(0);
				}).join();
				Assertions.assertEquals(expected.getAndIncrement(), value);
				if (!this.transactional) {
					// Grace time between fail-over requests
					TimeUnit.SECONDS.sleep(1);
				}
			}
		}
		this.client.sendAsync(HttpRequest.newBuilder(uri1).DELETE().build(), BodyHandlers.discarding()).thenAccept(response -> {
			assertEquals(HttpServletResponse.SC_OK, response.statusCode());
			assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none"));
		}).join();
		Thread.sleep(500);
		this.client.sendAsync(HttpRequest.newBuilder(uri2).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding()).thenAccept(response -> {
			assertEquals(HttpServletResponse.SC_OK, response.statusCode());
			assertNotEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none"));
		}).join();
	}
}
