/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A Spring Web session facade for a distributable session.
 * @author Paul Ferraro
 */
public class StartedWebSession implements SpringWebSession, Function<String, Void> {
	private static final System.Logger LOGGER = System.getLogger(StartedWebSession.class.getPackageName());

	private static void log(Throwable exception) {
		LOGGER.log(System.Logger.Level.WARNING, exception.getLocalizedMessage(), exception);
	}

	private final SessionManager<Void> manager;
	private final SuspendedBatch batch;
	private final AtomicReference<Runnable> closeTask;
	private final Instant startTime;

	private volatile Session<Void> session;

	/**
	 * Creates a distributable Spring Web session.
	 * @param manager the session manager associated with this session
	 * @param session the distributable session
	 * @param batch the batch associated with this session
	 * @param closeTask a task to run on session close.
	 */
	public StartedWebSession(SessionManager<Void> manager, Session<Void> session, SuspendedBatch batch, Runnable closeTask) {
		this.manager = manager;
		this.session = session;
		this.batch = batch;
		this.closeTask = new AtomicReference<>(closeTask);
		this.startTime = session.isValid() && session.getMetaData().getLastAccessTime().isEmpty() ? session.getMetaData().getCreationTime() : Instant.now();
	}

	@Override
	public String getId() {
		return this.session.getId();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.session.getAttributes();
	}

	@Override
	public void start() {
		// Already started
	}

	@Override
	public boolean isStarted() {
		return true;
	}

	@Override
	public boolean isValid() {
		return this.session.isValid();
	}

	@Override
	public Mono<Void> changeSessionId() {
		Mono<String> identifier = Mono.fromSupplier(this.manager.getIdentifierFactory()).publishOn(Schedulers.boundedElastic());
		return identifier.map(this).doOnError(StartedWebSession::log);
	}

	@Override
	public Void apply(String id) {
		Session<Void> oldSession = this.session;
		try (Context<Batch> context = this.batch.resumeWithContext()) {
			Session<Void> newSession = this.manager.createSession(id);
			try {
				for (Map.Entry<String, Object> entry : oldSession.getAttributes().entrySet()) {
					newSession.getAttributes().put(entry.getKey(), entry.getValue());
				}
				oldSession.getMetaData().getMaxIdle().ifPresent(newSession.getMetaData()::setMaxIdle);
				if (oldSession.getMetaData().getLastAccessTime().isPresent()) {
					newSession.getMetaData().setLastAccess(oldSession.getMetaData().getLastAccessStartTime().get(), oldSession.getMetaData().getLastAccessTime().get());
				}
				oldSession.invalidate();
				this.session = newSession;
				oldSession.close();
			} catch (IllegalStateException e) {
				newSession.invalidate();
				throw e;
			}
		}
		return null;
	}

	@Override
	public Mono<Void> invalidate() {
		return Mono.<Void>fromRunnable(this::invalidateSync)
				.publishOn(Schedulers.boundedElastic());
	}

	private void invalidateSync() {
		this.close(Session::invalidate);
	}

	@Override
	public Mono<Void> save() {
		// N.B. Poor interface design - this method should not be visible to the application
		// Fire-and-forget close
		return Mono.<Void>fromRunnable(() -> Mono.fromRunnable(this::closeSync)
				.doOnError(StartedWebSession::log)
				.subscribeOn(Schedulers.boundedElastic())
				.subscribe()).publishOn(Schedulers.boundedElastic());
	}

	void closeSync() {
		if (this.isValid()) {
			// According to §7.6 of the servlet specification:
			// The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
			this.close(session -> session.getMetaData().setLastAccess(this.startTime, Instant.now()));
		}
	}

	@Override
	public boolean isExpired() {
		return this.session.getMetaData().isExpired();
	}

	@Override
	public Instant getCreationTime() {
		return this.session.getMetaData().getCreationTime();
	}

	@Override
	public Instant getLastAccessTime() {
		return this.session.getMetaData().getLastAccessTime().orElse(this.session.getMetaData().getCreationTime());
	}

	@Override
	public void setMaxIdleTime(Duration maxIdleTime) {
		this.session.getMetaData().setMaxIdle(maxIdleTime);
	}

	@Override
	public Duration getMaxIdleTime() {
		return this.session.getMetaData().getMaxIdle().orElse(null);
	}

	private void close(Consumer<Session<Void>> action) {
		Runnable closeTask = this.closeTask.getAndSet(null);
		if (closeTask != null) {
			try (Context<Batch> context = this.batch.resumeWithContext()) {
				try (Batch batch = context.get()) {
					try (Session<Void> session = this.session) {
						if (session.isValid()) {
							action.accept(session);
						}
					}
				}
			} catch (RuntimeException | Error e) {
				log(e);
			} finally {
				closeTask.run();
			}
		}
	}
}
