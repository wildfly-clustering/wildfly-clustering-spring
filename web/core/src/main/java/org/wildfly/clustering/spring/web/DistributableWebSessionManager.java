/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Flux;
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

	private static void log(Throwable exception) {
		LOGGER.log(System.Logger.Level.ERROR, exception.getLocalizedMessage(), exception);
	}

	private final SessionManager<Void> manager;
	private final WebSessionIdResolver identifierResolver;
	private final StampedLock lifecycleLock = new StampedLock();

	public DistributableWebSessionManager(DistributableWebSessionManagerConfiguration configuration) {
		this.manager = configuration.getSessionManager();
		this.identifierResolver = configuration.getSessionIdentifierResolver();
		COUNTER.incrementAndGet();
	}

	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		return Flux.fromIterable(this.identifierResolver.resolveSessionIds(exchange))
				.flatMapSequential(this::findSessionPublisher)
				.filter(Objects::nonNull)
				.filter(SpringWebSession::isValid)
				.filter(Predicate.not(SpringWebSession::isExpired))
				.next()
				.switchIfEmpty(Mono.defer(this::createSessionPublisher))
				.doOnNext(session -> exchange.getResponse().beforeCommit(Supplier.of(Mono.fromRunnable(() -> {
					if (session.isStarted() && session.isValid()) {
						this.identifierResolver.setSessionId(exchange, session.getId());
					}
					if (!session.isStarted() || !session.isValid()) {
						this.identifierResolver.expireSession(exchange);
					}
					// Close session asynchronously
					Mono.fromRunnable(session::close)
							.publishOn(Schedulers.boundedElastic())
							.subscribeOn(Schedulers.boundedElastic())
							.subscribe();
				}))))
				.doOnError(DistributableWebSessionManager::log)
				.map(Function.identity());
	}

	private Mono<SpringWebSession> createSessionPublisher() {
		return Mono.fromSupplier(this.manager.getIdentifierFactory()).flatMap(id -> Mono.fromSupplier(this::createBatchEntry).flatMap(entry -> {
			SuspendedBatch batch = entry.getKey();
			Runnable closeTask = entry.getValue();
			try (BatchContext<Batch> context = batch.resumeWithContext()) {
				return Mono.fromCompletionStage(this.manager.createSessionAsync(id))
						.filter(Objects::nonNull)
						.map(session -> new DistributableWebSession(this.manager, session, batch, closeTask))
						.doOnError(DistributableWebSessionManager::log)
						.doOnError(e -> rollback(batch, closeTask));
			}
		}));
	}

	private Mono<SpringWebSession> findSessionPublisher(String id) {
		return Mono.fromSupplier(this::createBatchEntry).flatMap(entry -> {
			SuspendedBatch batch = entry.getKey();
			Runnable closeTask = entry.getValue();
			try (BatchContext<Batch> context = batch.resumeWithContext()) {
				return Mono.fromCompletionStage(this.manager.findSessionAsync(id))
						.map(session -> (session != null) ? new DistributableWebSession(this.manager, session, batch, closeTask) : rollback(batch, closeTask))
						.doOnError(DistributableWebSessionManager::log)
						.doOnError(e -> rollback(batch, closeTask));
			}
		});
	}

	@Override
	public void close() {
		try {
			this.lifecycleLock.writeLockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (COUNTER.decrementAndGet() == 0) {
			Schedulers.shutdownNow();
		}
	}

	private static <T> T rollback(SuspendedBatch suspendedBatch, Runnable closeTask) {
		try (Batch batch = suspendedBatch.resume()) {
			batch.discard();
		} catch (RuntimeException | Error e) {
			log(e);
		} finally {
			closeTask.run();
		}
		return null;
	}

	private Map.Entry<SuspendedBatch, Runnable> createBatchEntry() {
		Runnable closeTask = this.getSessionCloseTask();
		try {
			return Map.entry(this.manager.getBatchFactory().get().suspend(), closeTask);
		} catch (RuntimeException | Error e) {
			closeTask.run();
			throw e;
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
					lock.unlockRead(stamp);
				}
			}
		};
	}
}
