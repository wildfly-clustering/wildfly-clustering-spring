/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.util.MapEntry;
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
	private static final System.Logger LOGGER = System.getLogger(DistributableWebSessionManager.class.getPackageName());
	private static final AtomicInteger COUNTER = new AtomicInteger(0);

	private final SessionManager<Void> manager;
	private final WebSessionIdResolver identifierResolver;
	private final StampedLock lifecycleLock = new StampedLock();

	private volatile OptionalLong lifecycleStamp = OptionalLong.empty();

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
				.doOnNext(session -> exchange.getResponse().beforeCommit(Supplier.of(this.getCommitAction(exchange, session, requestedSessionId))))
				.map(Function.identity());
	}

	private Mono<SpringWebSession> getSession(BiFunction<SessionManager<Void>, String, CompletionStage<Session<Void>>> function, String id) {
		Mono<Map.Entry<SuspendedBatch, Runnable>> batchEntry = Mono.<Map.Entry<SuspendedBatch, Runnable>>fromSupplier(this::createBatchEntry).subscribeOn(Schedulers.boundedElastic());
		Mono<SpringWebSession> result = batchEntry.flatMap(entry -> {
			try (Batch batch = entry.getKey().resume()) {
				return Mono.fromCompletionStage(function.apply(this.manager, id))
						.map(session -> (session != null) ? new DistributableWebSession(this.manager, session, entry.getKey(), entry.getValue()) : null)
						.doOnError(e -> rollback(entry));
			}
		});
		Supplier<Mono<SpringWebSession>> creator = () -> batchEntry.flatMap(entry -> {
			try (Batch batch = entry.getKey().resume()) {
				return Mono.fromCompletionStage(this.manager.createSessionAsync(this.manager.getIdentifierFactory().get()))
						.map(session -> new DistributableWebSession(this.manager, session, entry.getKey(), entry.getValue()))
						.doOnError(e -> rollback(entry));
			}
		});
		return result.switchIfEmpty(Mono.defer(creator));
	}

	private Mono<Void> getCommitAction(ServerWebExchange exchange, SpringWebSession session, String requestedSessionId) {
		return Mono.<Void>fromRunnable(() -> {
			if ((requestedSessionId != null) && (!session.isStarted() || !session.isValid())) {
				this.identifierResolver.expireSession(exchange);
			} else if (requestedSessionId == null || !requestedSessionId.equals(session.getId())) {
				this.identifierResolver.setSessionId(exchange, session.getId());
			}
			// Close session asynchronously
			Mono.fromRunnable(session::close).subscribeOn(Schedulers.boundedElastic()).subscribe();
		});//.subscribeOn(Schedulers.boundedElastic());
	}

	private String requestedSessionId(ServerWebExchange exchange) {
		return this.identifierResolver.resolveSessionIds(exchange).stream().findFirst().orElse(null);
	}

	@Override
	public void close() {
		if (this.lifecycleStamp.isEmpty()) {
			try {
				this.lifecycleStamp = OptionalLong.of(this.lifecycleLock.writeLockInterruptibly());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (COUNTER.decrementAndGet() == 0) {
			Schedulers.shutdownNow();
		}
	}

	private MapEntry<SuspendedBatch, Runnable> createBatchEntry() {
		Runnable closeTask = this.getSessionCloseTask();
		try {
			return MapEntry.of(this.manager.getBatchFactory().get().suspend(), closeTask);
		} catch (RuntimeException | Error e) {
			closeTask.run();
			throw e;
		}
	}

	private static void rollback(Map.Entry<SuspendedBatch, Runnable> entry) {
		try (Batch batch = entry.getKey().resume()) {
			batch.discard();
		} catch (RuntimeException | Error e) {
			LOGGER.log(System.Logger.Level.WARNING, e.getLocalizedMessage(), e);
		} finally {
			entry.getValue().run();
		}
	}

	private Runnable getSessionCloseTask() {
		StampedLock lock = this.lifecycleLock;
		long stamp = lock.tryReadLock();
		if (!StampedLock.isReadLockStamp(stamp)) {
			throw new IllegalStateException();
		}
		AtomicLong stampRef = new AtomicLong(stamp);
		return new Runnable() {
			@Override
			public void run() {
				// Ensure we only unlock once.
				long stamp = stampRef.getAndSet(0L);
				if (StampedLock.isReadLockStamp(stamp)) {
					lock.unlock(stamp);
				}
			}
		};
	}
}
