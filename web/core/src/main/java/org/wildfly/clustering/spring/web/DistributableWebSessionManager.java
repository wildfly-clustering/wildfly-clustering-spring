/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.common.function.Functions;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A distributable {@link WebSessionManager} implementation.
 * The avoidance of {@link org.springframework.web.server.session.DefaultWebSessionManager} with SpringSessionWebSessionStore is intentional,
 * since that implementation is unsafe for modification by multiple threads.
 * @author Paul Ferraro
 */
public class DistributableWebSessionManager implements WebSessionManager, AutoCloseable {
	private static final Logger LOGGER = Logger.getLogger(DistributableWebSessionManager.class);
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
		return this.getSession(function, sessionId).filter(Objects::nonNull)
				.doOnNext(session -> exchange.getResponse().beforeCommit(Functions.constantSupplier(this.getCommitAction(exchange, session, requestedSessionId))))
				.map(Function.identity());
	}

	private Mono<SpringWebSession> getSession(BiFunction<SessionManager<Void>, String, CompletionStage<Session<Void>>> function, String id) {
		Batch batch = this.manager.getBatchFactory().get();
		try {
			Mono<Session<Void>> result = Mono.fromCompletionStage(function.apply(this.manager, id)).doOnError(DistributableWebSessionManager::log);
			SuspendedBatch suspendedBatch = batch.suspend();
			Supplier<Mono<Session<Void>>> creator = () -> {
				try (BatchContext<Batch> context = suspendedBatch.resumeWithContext()) {
					return Mono.fromCompletionStage(this.manager.createSessionAsync(this.manager.getIdentifierFactory().get())).doOnError(DistributableWebSessionManager::log);
				}
			};
			return result.switchIfEmpty(Mono.defer(creator)).doOnError(e -> rollback(suspendedBatch.resume()))
					.map(session -> new DistributableWebSession(this.manager, session, suspendedBatch));
		} catch (RuntimeException | Error e) {
			rollback(batch);
			return Mono.error(e);
		}
	}

	private static void rollback(Batch resumedBatch) {
		try (Batch batch = resumedBatch) {
			batch.discard();
		} catch (RuntimeException | Error e) {
			log(e);
		}
	}

	private static void log(Throwable e) {
		LOGGER.warn(e.getLocalizedMessage(), e);
	}

	private Mono<Void> getCommitAction(ServerWebExchange exchange, SpringWebSession session, String requestedSessionId) {
		return Mono.fromRunnable(() -> {
			if ((requestedSessionId != null) && (!session.isStarted() || !session.isValid())) {
				this.identifierResolver.expireSession(exchange);
			} else if (requestedSessionId == null || !requestedSessionId.equals(session.getId())) {
				this.identifierResolver.setSessionId(exchange, session.getId());
			}
			// Close session asynchronously
			Mono.fromRunnable(session::close).doOnError(DistributableWebSessionManager::log).subscribeOn(Schedulers.boundedElastic()).subscribe();
		});
	}

	private String requestedSessionId(ServerWebExchange exchange) {
		return this.identifierResolver.resolveSessionIds(exchange).stream().findFirst().orElse(null);
	}

	@Override
	public void close() {
		if (COUNTER.decrementAndGet() == 0) {
			Schedulers.shutdownNow();
		}
	}
}
