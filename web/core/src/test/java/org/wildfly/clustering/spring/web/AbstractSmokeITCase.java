/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
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

	private static final int ITERATIONS = 4;
	private static final int CONCURRENCY = ITERATIONS * 10;

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
				.addClasses(PropertiesAsset.class, SmokeITParameters.class)
				;
	}

	private final boolean transactional;
	private final ExecutorService executor;
	private final HttpClient client;

	protected AbstractSmokeITCase(HttpClient.Builder builder) {
		this(false, builder);
	}

	protected AbstractSmokeITCase(boolean transactional, HttpClient.Builder builder) {
		this.transactional = transactional;
		this.executor = Executors.newFixedThreadPool(CONCURRENCY);
		this.client = builder.cookieHandler(new CookieManager()).executor(this.executor).build();
	}

	@AfterEach
	public void destroy() {
		this.executor.shutdown();
	}

	@Override
	public void accept(URI baseURI1, URI baseURI2) throws Exception {
		URI uri1 = baseURI1.resolve(SmokeITParameters.ENDPOINT_NAME);
		URI uri2 = baseURI2.resolve(SmokeITParameters.ENDPOINT_NAME);
		for (URI uri : Arrays.asList(uri1, uri2)) {
			// Verify a request that never starts its session
			this.client.sendAsync(HttpRequest.newBuilder(uri).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding()).thenAccept(response -> {
				assertEquals(HttpServletResponse.SC_OK, response.statusCode());
				assertNull(response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse(null));
				assertFalse(response.headers().firstValueAsLong(SmokeITParameters.VALUE).isPresent());
			}).join();
		}
		AtomicReference<String> sessionId = new AtomicReference<>();
		AtomicLong expected = new AtomicLong(0);
		for (int i = 0; i < ITERATIONS; i++) {
			for (URI uri : Arrays.asList(uri1, uri2)) {
				int count = i;
				long value = this.client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.discarding()).thenApply(response -> {
					assertEquals(HttpServletResponse.SC_OK, response.statusCode(), Integer.toString(count));
					String requestSessionId = response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse(null);
					assertNotNull(requestSessionId);
					// Validate propagation of session ID
					if (!sessionId.compareAndSet(null, requestSessionId)) {
						assertEquals(sessionId.get(), requestSessionId);
					}
					return response.headers().firstValueAsLong(SmokeITParameters.VALUE).orElse(0);
				}).join();
				Assertions.assertEquals(expected.incrementAndGet(), value);
				// Validate session is still "started"
				this.client.sendAsync(HttpRequest.newBuilder(uri).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding()).thenAccept(response -> {
					assertEquals(HttpServletResponse.SC_OK, response.statusCode());
					assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse(null));
				}).join();
				// Perform a number of concurrent requests incrementing the session attribute
				List<CompletableFuture<Long>> futures = new ArrayList<>(CONCURRENCY);
				for (int j = 0; j < CONCURRENCY; j++) {
					CompletableFuture<Long> future = this.client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.discarding()).thenApply(response -> {
						assertEquals(HttpServletResponse.SC_OK, response.statusCode());
						assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse(null));
						return response.headers().firstValueAsLong(SmokeITParameters.VALUE).orElse(0);
					});
					futures.add(future);
				}
				expected.addAndGet(CONCURRENCY);
				// Verify the correct number of unique results
				assertEquals(CONCURRENCY, futures.stream().map(CompletableFuture::join).distinct().count());
				// Verify expected session attribute value following concurrent updates
				value = this.client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.discarding()).thenApply(response -> {
					assertEquals(HttpServletResponse.SC_OK, response.statusCode());
					assertEquals(sessionId.get(), response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse("none"));
					return response.headers().firstValueAsLong(SmokeITParameters.VALUE).orElse(0);
				}).join();
				Assertions.assertEquals(expected.incrementAndGet(), value);
				if (!this.transactional) {
					// Grace time between fail-over requests
					TimeUnit.SECONDS.sleep(1);
				}
			}
		}
		// Invalidate session
		this.client.sendAsync(HttpRequest.newBuilder(uri1).DELETE().build(), BodyHandlers.discarding()).thenAccept(response -> {
			assertEquals(HttpServletResponse.SC_OK, response.statusCode());
			assertNull(response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse(null));
		}).join();
		List<CompletableFuture<Void>> futures = new ArrayList<>(2);
		for (URI uri : Arrays.asList(uri1, uri2)) {
			// Verify session was truly invalidated
			futures.add(this.client.sendAsync(HttpRequest.newBuilder(uri).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding()).thenAccept(response -> {
				assertEquals(HttpServletResponse.SC_OK, response.statusCode());
				assertNull(response.headers().firstValue(SmokeITParameters.SESSION_ID).orElse(null));
				assertFalse(response.headers().firstValueAsLong(SmokeITParameters.VALUE).isPresent());
			}));
		}
		futures.stream().forEach(CompletableFuture::join);
	}
}
