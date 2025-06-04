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
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Paul Ferraro
 */
public class DistributableWebSession implements SpringWebSession, Function<String, Void> {
	private static final System.Logger LOGGER = System.getLogger(DistributableWebSession.class.getPackageName());

	private static void log(Throwable exception) {
		LOGGER.log(System.Logger.Level.WARNING, exception.getLocalizedMessage(), exception);
	}

	private final SessionManager<Void> manager;
	private final SuspendedBatch batch;
	private final AtomicReference<Runnable> closeTask;
	private final Instant startTime;

	private volatile boolean started;
	private volatile Session<Void> session;

	public DistributableWebSession(SessionManager<Void> manager, Session<Void> session, SuspendedBatch batch, Runnable closeTask) {
		this.manager = manager;
		this.session = session;
		this.started = session.isValid() && !session.getMetaData().isNew();
		this.batch = batch;
		this.closeTask = new AtomicReference<>(closeTask);
		this.startTime = session.isValid() && session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
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
		this.started = true;
	}

	@Override
	public boolean isStarted() {
		return this.started;
	}

	@Override
	public boolean isNew() {
		return this.session.getMetaData().isNew();
	}

	@Override
	public boolean isValid() {
		return this.session.isValid();
	}

	@Override
	public Mono<Void> changeSessionId() {
		Mono<String> identifier = Mono.fromSupplier(this.manager.getIdentifierFactory());
		return identifier.map(this).doOnError(DistributableWebSession::log);
	}

	@Override
	public Void apply(String id) {
		Session<Void> oldSession = this.session;
		try (BatchContext<Batch> context = this.batch.resumeWithContext()) {
			Session<Void> newSession = this.manager.createSession(id);
			try {
				for (Map.Entry<String, Object> entry : oldSession.getAttributes().entrySet()) {
					newSession.getAttributes().put(entry.getKey(), entry.getValue());
				}
				newSession.getMetaData().setTimeout(oldSession.getMetaData().getTimeout());
				newSession.getMetaData().setLastAccess(oldSession.getMetaData().getLastAccessStartTime(), oldSession.getMetaData().getLastAccessTime());
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
		return Mono.<Void>fromRunnable(this::invalidateSync).subscribeOn(Schedulers.boundedElastic());
	}

	private void invalidateSync() {
		this.close(Session::invalidate);
	}

	@Override
	public void close() {
		if (this.started) {
			// According to §7.6 of the servlet specification:
			// The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
			this.close(session -> session.getMetaData().setLastAccess(this.startTime, Instant.now()));
		} else {
			// Invalidate if session was never "started".
			this.invalidateSync();
		}
	}

	@Override
	public Mono<Void> save() {
		// This behavior is implemented via close(), to prevent applications from manipulating session lifecycle
		return Mono.empty();
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
		return this.session.getMetaData().getLastAccessTime();
	}

	@Override
	public void setMaxIdleTime(Duration maxIdleTime) {
		this.session.getMetaData().setTimeout(maxIdleTime);
	}

	@Override
	public Duration getMaxIdleTime() {
		return this.session.getMetaData().getTimeout();
	}

	private void close(Consumer<Session<Void>> action) {
		Runnable closeTask = this.closeTask.getAndSet(null);
		if (closeTask != null) {
			try (Batch batch = this.batch.resume()) {
				try (Session<Void> session = this.session) {
					if (session.isValid()) {
						action.accept(session);
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
