/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Runnable;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A distributable {@link org.springframework.web.server.WebSession} is not created until saved.
 * @author Paul Ferraro
 */
public class LazyWebSession implements SpringWebSession {
	private final SessionManager<Void> manager;
	private final AtomicReference<String> id;
	private final AtomicBoolean valid = new AtomicBoolean(true);
	private final AtomicReference<Instant> start = new AtomicReference<>();
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();
	private final AtomicReference<Optional<Duration>> timeout = new AtomicReference<>(Optional.empty());
	private final Runnable closeTask;

	/**
	 * Creates a new web session.
	 * @param manager the associated session manager
	 * @param id the identifier of the new session
	 * @param closeTask a task to run on {@link #save()}
	 */
	public LazyWebSession(SessionManager<Void> manager, String id, Runnable closeTask) {
		this.manager = manager;
		this.id = new AtomicReference<>(id);
		this.closeTask = closeTask;
	}

	@Override
	public String getId() {
		return this.id.get();
	}

	@Override
	public Map<String, Object> getAttributes() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		return this.attributes;
	}

	@Override
	public void start() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		this.start.set(Instant.now());
	}

	@Override
	public boolean isStarted() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		// According to the API documentation, a session many be started explicitly or implicitly, i.e. via start() or by adding an attribute, respectively.
		// The default InMemoryWebSession implementation, however, does not actually comply with this documentation - and only detects an implicit start during save() by checking the number of attributes.
		// Therefore, we will instead assume that the API documentation is incorrect, and follow the behaviour of InMemorySession.
		return this.start.get() != null || !this.attributes.isEmpty();
	}

	@Override
	public Mono<Void> changeSessionId() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		return Mono.fromRunnable(Runnable.accept(this.id::set, this.manager.getIdentifierFactory()));
	}

	@Override
	public Mono<Void> invalidate() {
		if (!this.valid.compareAndSet(true, false)) {
			throw new IllegalStateException();
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> save() {
		// N.B. Poor interface design - this method should not be visible to the application
		if (!this.isStarted() || !this.isValid()) {
			return Mono.fromRunnable(this.closeTask);
		}
		SuspendedBatch suspended = this.manager.getBatchFactory().get().suspend();
		try (Context<Batch> context = suspended.resumeWithContext()) {
			return Mono.fromCompletionStage(this.manager.createSessionAsync(this.id.get()))
					.publishOn(Schedulers.boundedElastic())
					.doOnNext(newSession -> {
						try (Context<Batch> resumed = suspended.resumeWithContext()) {
							try (Batch batch = resumed.get()) {
								try (Session<Void> session = newSession) {
									session.getAttributes().putAll(this.attributes);
									SessionMetaData metaData = session.getMetaData();
									this.timeout.get().ifPresent(metaData::setTimeout);
									metaData.setLastAccess(metaData.getCreationTime(), Instant.now());
								}
							}
						}
					}).then().doOnError(exception -> {
						try (Batch resumed = suspended.resume()) {
							resumed.discard();
						}
					}).doAfterTerminate(this.closeTask);
		}
	}

	@Override
	public boolean isExpired() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		return false;
	}

	@Override
	public Instant getCreationTime() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		return this.start.get();
	}

	@Override
	public Instant getLastAccessTime() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		return null;
	}

	@Override
	public void setMaxIdleTime(Duration maxIdleTime) {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		this.timeout.set(Optional.ofNullable(maxIdleTime));
	}

	@Override
	public Duration getMaxIdleTime() {
		if (!this.valid.get()) {
			throw new IllegalStateException();
		}
		return this.timeout.get().orElse(null);
	}

	@Override
	public boolean isValid() {
		return this.valid.get();
	}
}
