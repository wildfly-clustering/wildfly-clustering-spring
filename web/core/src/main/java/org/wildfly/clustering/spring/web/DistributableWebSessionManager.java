/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.util.Iterator;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A distributable {@link WebSessionManager} implementation.
 * The avoidance of {@link org.springframework.web.server.session.DefaultWebSessionManager} with SpringSessionWebSessionStore is intentional,
 * since that implementation is unsafe for modification by multiple threads.
 * @author Paul Ferraro
 */
public class DistributableWebSessionManager implements WebSessionManager, AutoCloseable {
	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final SessionManager<Void> manager;
	private final WebSessionIdResolver identifierResolver;

	public DistributableWebSessionManager(DistributableWebSessionManagerConfiguration configuration) {
		this.manager = configuration.getSessionManager();
		this.identifierResolver = configuration.getSessionIdentifierResolver();
		COUNTER.incrementAndGet();
	}

	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		String requestedSessionId = this.requestedSessionId(exchange);
		String sessionId = (requestedSessionId != null) ? requestedSessionId : this.manager.getIdentifierFactory().get();
		BiFunction<SessionManager<Void>, String, CompletionStage<Session<Void>>> function = (requestedSessionId != null) ? SessionManager::findSessionAsync : SessionManager::createSessionAsync;
		return this.getSession(function, sessionId).doOnNext(session -> exchange.getResponse().beforeCommit(() -> this.close(exchange, session))).map(Function.identity());
	}

	private Mono<SpringWebSession> getSession(BiFunction<SessionManager<Void>, String, CompletionStage<Session<Void>>> function, String id) {
		Batch batch = this.manager.getBatchFactory().get();
		try {
			Mono<Session<Void>> result = Mono.fromCompletionStage(function.apply(this.manager, id)).doOnError(Throwable::printStackTrace);
			SuspendedBatch suspendedBatch = batch.suspend();
			Supplier<Mono<Session<Void>>> creator = () -> {
				try (BatchContext<Batch> context = suspendedBatch.resumeWithContext()) {
					return Mono.fromCompletionStage(this.manager.createSessionAsync(this.manager.getIdentifierFactory().get())).subscribeOn(Schedulers.boundedElastic());
				}
			};
			return result.switchIfEmpty(Mono.defer(creator)).doOnError(e -> rollback(suspendedBatch.resume())).map(session -> new DistributableWebSession(this.manager, session, suspendedBatch));
		} catch (RuntimeException | Error e) {
			rollback(batch);
			throw e;
		}
	}

	private static void rollback(Batch resumedBatch) {
		try (Batch batch = resumedBatch) {
			batch.discard();
		}
	}

	private Mono<Void> close(ServerWebExchange exchange, SpringWebSession session) {
		String requestedSessionId = this.requestedSessionId(exchange);

		if ((requestedSessionId != null) && (!session.isStarted() || !session.isValid())) {
			this.identifierResolver.expireSession(exchange);
		} else if (requestedSessionId == null || !requestedSessionId.equals(session.getId())) {
			this.identifierResolver.setSessionId(exchange, session.getId());
		}

		return Mono.<Void>fromRunnable(session::close).doOnError(Throwable::printStackTrace).subscribeOn(Schedulers.boundedElastic());
	}

	private String requestedSessionId(ServerWebExchange exchange) {
		Iterator<String> sessionIds = this.identifierResolver.resolveSessionIds(exchange).iterator();
		return sessionIds.hasNext() ? sessionIds.next() : null;
	}

	@Override
	public void close() {
		if (COUNTER.decrementAndGet() == 0) {
			Schedulers.shutdownNow();
		}
	}
}
