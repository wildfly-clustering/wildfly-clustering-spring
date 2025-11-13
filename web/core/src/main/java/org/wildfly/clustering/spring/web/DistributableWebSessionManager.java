/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.Runnable;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A Spring Web session manager facade for a distributable session manager.
 * The avoidance of {@link org.springframework.web.server.session.DefaultWebSessionManager} with SpringSessionWebSessionStore is intentional,
 * since that implementation is unsafe for modification by multiple threads.
 * @author Paul Ferraro
 */
public class DistributableWebSessionManager implements WebSessionManager, DisposableBean {
	private static final System.Logger LOGGER = System.getLogger(DistributableWebSessionManager.class.getPackageName());
	private static final AtomicInteger COUNTER = new AtomicInteger(0);
	private static final Predicate<Session<Void>> IS_VALID = Session::isValid;

	private static void log(Throwable exception) {
		LOGGER.log(System.Logger.Level.ERROR, exception.getLocalizedMessage(), exception);
	}

	private final SessionManager<Void> manager;
	private final WebSessionIdResolver identifierResolver;
	private final StampedLock lifecycleLock = new StampedLock();

	/**
	 * Creates a Spring Web session manager from the specified configuration.
	 * @param configuration a session manager configuration
	 */
	public DistributableWebSessionManager(DistributableWebSessionManagerConfiguration configuration) {
		this.manager = configuration.getSessionManager();
		this.identifierResolver = configuration.getSessionIdentifierResolver();
		COUNTER.incrementAndGet();
	}

	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		return Flux.fromIterable(this.identifierResolver.resolveSessionIds(exchange))
				.publishOn(Schedulers.boundedElastic())
				.flatMapSequential(this::findSessionPublisher)
				.next()
				.switchIfEmpty(Mono.fromSupplier(this.manager.getIdentifierFactory()).map(id -> new LazyWebSession(this.manager, id, this.getSessionCloseTask())))
				.doOnNext(session -> exchange.getResponse().beforeCommit(Supplier.of(Mono.defer(() -> {
					if (session.isStarted() && session.isValid()) {
						this.identifierResolver.setSessionId(exchange, session.getId());
					} else {
						this.identifierResolver.expireSession(exchange);
					}
					return session.save().doOnError(DistributableWebSessionManager::log);
				}))))
				.doOnError(DistributableWebSessionManager::log)
				.map(Function.identity());
	}

	private Mono<SpringWebSession> findSessionPublisher(String id) {
		return this.getSessionPublisher(Supplier.of(id).thenApply(this.manager::findSessionAsync));
	}

	private Mono<SpringWebSession> getSessionPublisher(Supplier<CompletionStage<Session<Void>>> factory) {
		Map.Entry<SuspendedBatch, Runnable> entry = this.createBatchEntry();
		try (Context<Batch> context = entry.getKey().resumeWithContext()) {
			return Mono.fromCompletionStage(factory.get())
					.filter(IS_VALID)
					.<SpringWebSession>map(session -> new StartedWebSession(this.manager, session, entry.getKey(), entry.getValue()))
					.switchIfEmpty(Mono.defer(() -> {
						close(entry, Consumer.empty());
						return Mono.empty();
					}))
					.doOnError(e -> rollback(entry));
		}
	}

	@Override
	public void destroy() {
		try {
			this.lifecycleLock.writeLockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (COUNTER.decrementAndGet() == 0) {
			Schedulers.shutdownNow();
		}
	}

	private static void rollback(Map.Entry<SuspendedBatch, Runnable> entry) {
		close(entry, Batch::discard);
	}

	private static void close(Map.Entry<SuspendedBatch, Runnable> entry, Consumer<Batch> batchTask) {
		try (Context<Batch> context = entry.getKey().resumeWithContext()) {
			try (Batch batch = context.get()) {
				batchTask.accept(batch);
			}
		} catch (RuntimeException | Error e) {
			LOGGER.log(System.Logger.Level.WARNING, e.getLocalizedMessage(), e);
		} finally {
			entry.getValue().run();
		}
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
