/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.Batcher;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A distributable {@link WebSessionManager} implementation.
 * The avoidance of {@link org.springframework.web.server.session.DefaultWebSessionManager} with {@link org.springframework.session.web.server.session.SpringSessionWebSessionStore} is intentional,
 * since that implementation is unsafe for modification by multiple threads.
 * @author Paul Ferraro
 */
public class DistributableWebSessionManager<B extends Batch> implements WebSessionManager {

	private final SessionManager<Void, B> manager;
	private final WebSessionIdResolver identifierResolver;

	public DistributableWebSessionManager(DistributableWebSessionManagerConfiguration<B> configuration) {
		this.manager = configuration.getSessionManager();
		this.identifierResolver = configuration.getSessionIdentifierResolver();
	}

	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		String requestedSessionId = this.requestedSessionId(exchange);
		String sessionId = (requestedSessionId != null) ? requestedSessionId : this.manager.getIdentifierFactory().get();
		BiFunction<SessionManager<Void, B>, String, CompletionStage<Session<Void>>> function = (requestedSessionId != null) ? SessionManager::findSessionAsync : SessionManager::createSessionAsync;
		return this.getSession(function, sessionId).doOnNext(session -> exchange.getResponse().beforeCommit(() -> this.close(exchange, session))).map(Function.identity());
	}

	private Mono<SpringWebSession> getSession(BiFunction<SessionManager<Void, B>, String, CompletionStage<Session<Void>>> function, String id) {
		Batcher<B> batcher = this.manager.getBatcher();
		B batch = batcher.createBatch();
		try {
			Mono<Session<Void>> result = Mono.fromCompletionStage(function.apply(this.manager, id));
//			CompletionStage<Session<Void>> future = function.apply(this.manager, id);
			B suspendedBatch = batcher.suspendBatch();
			Supplier<Mono<Session<Void>>> creator = () -> {
				try (BatchContext context = this.manager.getBatcher().resumeBatch(suspendedBatch)) {
					return Mono.fromCompletionStage(this.manager.createSessionAsync(this.manager.getIdentifierFactory().get())).publishOn(Schedulers.boundedElastic());
				}
			};
			return result.flatMap(session -> Optional.ofNullable(session).map(Mono::just).orElseGet(creator))
					.flatMap(session -> Mono.just(new DistributableWebSession<>(this.manager, session, suspendedBatch)));
//			return Mono.fromCompletionStage(future.thenCompose(session -> Optional.ofNullable(session).map(CompletableFuture::completedStage).orElseGet(creator))
//					.thenApply(session -> new DistributableWebSession<>(this.manager, session, suspendedBatch)));
		} catch (RuntimeException | Error e) {
			try (BatchContext context = batcher.resumeBatch(batch)) {
				batch.discard();
			}
			throw e;
		}
	}

	private Mono<Void> close(ServerWebExchange exchange, SpringWebSession session) {
		String requestedSessionId = this.requestedSessionId(exchange);

		if ((requestedSessionId != null) && (!session.isStarted() || !session.isValid())) {
			this.identifierResolver.expireSession(exchange);
		} else if (requestedSessionId == null || !requestedSessionId.equals(session.getId())) {
			this.identifierResolver.setSessionId(exchange, session.getId());
		}

		return Mono.<Void>fromRunnable(session::close).publishOn(Schedulers.boundedElastic());
	}

	private String requestedSessionId(ServerWebExchange exchange) {
		Iterator<String> sessionIds = this.identifierResolver.resolveSessionIds(exchange).iterator();
		return sessionIds.hasNext() ? sessionIds.next() : null;
	}
}
