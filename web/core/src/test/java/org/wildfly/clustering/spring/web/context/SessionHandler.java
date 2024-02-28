/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.context;

import java.util.concurrent.atomic.AtomicInteger;

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
public class SessionHandler implements WebHandler {

	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		ServerHttpResponse response = exchange.getResponse();
		Mono<WebSession> publisher = exchange.getSession().doOnNext(session -> response.getHeaders().set(SmokeITParameters.SESSION_ID, session.getId()));
		if (method.equals(HttpMethod.GET)) {
			publisher = publisher.doOnNext(session -> {
				AtomicInteger value = session.getAttribute(SmokeITParameters.VALUE);
				int result = 0;
				if (value == null) {
					value = new AtomicInteger(result);
					session.getAttributes().put(SmokeITParameters.VALUE, value);
				} else {
					result = value.incrementAndGet();
				}
				response.getHeaders().set(SmokeITParameters.VALUE, Integer.toString(result));
			});
		} else if (method.equals(HttpMethod.DELETE)) {
			return publisher.flatMap(WebSession::invalidate);
		} else if (!method.equals(HttpMethod.HEAD)) {
			response.setStatusCode(HttpStatusCode.valueOf(HttpServletResponse.SC_METHOD_NOT_ALLOWED));
		}
		return Mono.when(publisher);
	}
}
