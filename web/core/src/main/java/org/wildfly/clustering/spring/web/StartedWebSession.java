/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.server.util.BlockingReference;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A Spring Web session facade for a distributable session.
 * @author Paul Ferraro
 */
public class StartedWebSession implements SpringWebSession, Function<String, Void> {
	private static final System.Logger LOGGER = System.getLogger(StartedWebSession.class.getPackageName());

	private final SessionManager<Void> manager;
	private final BlockingReference<Session<Void>> reference;
	private final AtomicReference<Runnable> closeTask;
	private final Instant startTime;

	/**
	 * Creates a distributable Spring Web session.
	 * @param manager the session manager associated with this session
	 * @param session the distributable session
	 * @param closeTask a task to run on session close.
	 */
	public StartedWebSession(SessionManager<Void> manager, Session<Void> session, Runnable closeTask) {
		this.manager = manager;
		this.reference = BlockingReference.of(session);
		this.closeTask = new AtomicReference<>(closeTask);
		this.startTime = session.isValid() && session.getMetaData().getLastAccessTime().isEmpty() ? session.getMetaData().getCreationTime() : Instant.now();
	}

	@Override
	public String getId() {
		return this.reference.getReader().map(ImmutableSession.IDENTIFIER).get();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.reference.getReader().map(ImmutableSession.ATTRIBUTES).get();
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
		return this.reference.getReader().map(ImmutableSession.VALID.thenBox()).get();
	}

	@Override
	public Mono<Void> changeSessionId() {
		Mono<String> identifier = Mono.fromSupplier(this.manager.getIdentifierFactory()).publishOn(Schedulers.boundedElastic());
		return identifier.map(this).doOnError(StartedWebSession::log);
	}

	@Override
	public Void apply(String id) {
		this.reference.getWriter().update(currentSession -> {
			SessionMetaData currentMetaData = currentSession.getMetaData();
			Map<String, Object> currentAttributes = currentSession.getAttributes();
			Session<Void> newSession = this.manager.createSession(id);
			try {
				newSession.getAttributes().putAll(currentAttributes);
				SessionMetaData newMetaData = newSession.getMetaData();
				currentMetaData.getMaxIdle().ifPresent(newMetaData::setMaxIdle);
				currentMetaData.getLastAccess().ifPresent(newMetaData::setLastAccess);
				currentSession.invalidate();
				return newSession;
			} catch (RuntimeException | Error e) {
				newSession.invalidate();
				throw e;
			} finally {
				Consumer.close().accept(newSession.isValid() ? currentSession : newSession);
			}
		});
		return null;
	}

	@Override
	public Mono<Void> invalidate() {
		return Mono.<Void>fromRunnable(this::invalidateSync)
				.publishOn(Schedulers.boundedElastic());
	}

	private void invalidateSync() {
		Runnable closeTask = this.closeTask.getAndSet(null);
		if (closeTask != null) {
			try {
				this.reference.getReader().read(invalidSession -> {
					try (Session<Void> session = invalidSession) {
						invalidSession.invalidate();
					}
				});
			} finally {
				closeTask.run();
			}
		}
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
		Runnable closeTask = this.closeTask.getAndSet(null);
		if (closeTask != null) {
			try {
				this.reference.getReader().read(completeSession -> {
					try (Session<Void> session = completeSession) {
						if (session.isValid()) {
							session.getMetaData().setLastAccess(this.startTime, Instant.now());
						}
					}
				});
			} finally {
				closeTask.run();
			}
		}
	}

	@Override
	public boolean isExpired() {
		return this.reference.getReader().map(ImmutableSession.METADATA).map(ImmutableSessionMetaData::isExpired).get();
	}

	@Override
	public Instant getCreationTime() {
		return this.reference.getReader().map(ImmutableSession.METADATA).map(ImmutableSessionMetaData.CREATION_TIME).get();
	}

	@Override
	public Instant getLastAccessTime() {
		return this.reference.getReader().map(ImmutableSession.METADATA).map(ImmutableSessionMetaData.LAST_ACCESS_TIME).get();
	}

	@Override
	public void setMaxIdleTime(Duration maxIdle) {
		this.reference.getReader().map(Session.METADATA).read(SessionMetaData.MAX_IDLE.composeUnary(Function.identity(), Function.of(maxIdle)));
	}

	@Override
	public Duration getMaxIdleTime() {
		return this.reference.getReader().map(ImmutableSession.METADATA).map(ImmutableSessionMetaData.MAX_IDLE).get().orElse(Duration.ZERO);
	}

	private static void log(Throwable exception) {
		LOGGER.log(System.Logger.Level.WARNING, exception.getLocalizedMessage(), exception);
	}
}
