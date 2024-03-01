/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.context;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.WebSession;
import org.wildfly.clustering.spring.web.SmokeITParameters;

import reactor.core.publisher.Mono;

/**
 * @author Paul Ferraro
 */
public class SessionHandler implements WebHandler, Function<ServerWebExchange, Mono<WebSession>> {
	private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.DELETE);

	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		if (!SUPPORTED_METHODS.contains(method)) {
			response.setStatusCode(HttpStatusCode.valueOf(HttpServletResponse.SC_METHOD_NOT_ALLOWED));
			return Mono.empty();
		}
		Mono<WebSession> sessionPublisher = this.apply(exchange);
		if (method.equals(HttpMethod.DELETE)) {
			return sessionPublisher.flatMap(WebSession::invalidate);
		}
		return sessionPublisher.doOnNext(session -> {
			if (session.isStarted()) {
				response.getHeaders().set(SmokeITParameters.SESSION_ID, session.getId());
			}
		}).then();
	}

	@Override
	public Mono<WebSession> apply(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		ServerHttpResponse response = exchange.getResponse();
		Mono<WebSession> sessionPublisher = exchange.getSession();
		if (method.equals(HttpMethod.GET)) {
			return sessionPublisher.doOnNext(session -> {
				AtomicInteger value = (AtomicInteger) session.getAttributes().computeIfAbsent(SmokeITParameters.VALUE, key -> new AtomicInteger(0));
				response.getHeaders().set(SmokeITParameters.VALUE, Integer.toString(value.incrementAndGet()));
			});
		}
		return sessionPublisher;
	}
}
