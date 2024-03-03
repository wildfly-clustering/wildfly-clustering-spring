/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.context;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.WebSession;
import org.wildfly.clustering.session.container.SessionManagementEndpointConfiguration;

import reactor.core.publisher.Mono;

/**
 * @author Paul Ferraro
 */
public class SessionHandler implements WebHandler, Function<ServerWebExchange, Mono<WebSession>> {
	private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(HttpMethod.HEAD, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE);

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
				response.getHeaders().set(SessionManagementEndpointConfiguration.SESSION_ID, session.getId());
			}
		}).then();
	}

	@Override
	public Mono<WebSession> apply(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		ServerHttpResponse response = exchange.getResponse();
		Mono<WebSession> sessionPublisher = exchange.getSession();
		if (method.equals(HttpMethod.PUT)) {
			return sessionPublisher.doOnNext(WebSession::start).doOnNext(session -> {
				UUID immutableValue = UUID.randomUUID();
				session.getAttributes().put(SessionManagementEndpointConfiguration.IMMUTABLE, immutableValue);
				response.getHeaders().set(SessionManagementEndpointConfiguration.IMMUTABLE, immutableValue.toString());

				AtomicInteger counter = new AtomicInteger(0);
				session.getAttributes().put(SessionManagementEndpointConfiguration.COUNTER, counter);
				response.getHeaders().set(SessionManagementEndpointConfiguration.COUNTER, Integer.toString(counter.get()));
			});
		}
		ToIntFunction<AtomicInteger> count = method.equals(HttpMethod.GET) ? AtomicInteger::incrementAndGet : AtomicInteger::get;
		return sessionPublisher.doOnNext(session -> {
			UUID value = (UUID) session.getAttribute(SessionManagementEndpointConfiguration.IMMUTABLE);
			if (value != null) {
				response.getHeaders().set(SessionManagementEndpointConfiguration.IMMUTABLE, value.toString());
			}

			AtomicInteger counter = (AtomicInteger) session.getAttribute(SessionManagementEndpointConfiguration.COUNTER);
			if (counter != null) {
				response.getHeaders().set(SessionManagementEndpointConfiguration.COUNTER, Integer.toString(count.applyAsInt(counter)));
			}
		});
	}
}
