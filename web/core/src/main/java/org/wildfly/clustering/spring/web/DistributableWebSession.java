/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Paul Ferraro
 */
public class DistributableWebSession<B extends Batch> implements SpringWebSession {

	private final SessionManager<Void, B> manager;
	private final B batch;
	private final Instant startTime;

	private volatile boolean started;
	private volatile Session<Void> session;

	public DistributableWebSession(SessionManager<Void, B> manager, Session<Void> session, B batch) {
		this.manager = manager;
		this.session = session;
		this.started = !session.getMetaData().isNew();
		this.batch = batch;
		this.startTime = Instant.now();
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
		Session<Void> oldSession = this.session;
		String id = this.manager.getIdentifierFactory().get();
		return Mono.fromRunnable(() -> {
			try (BatchContext<B> context = this.manager.getBatcher().resumeBatch(this.batch)) {
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
		});
	}

	@Override
	public Mono<Void> invalidate() {
		return Mono.<Void>fromRunnable(this::invalidateSync).subscribeOn(Schedulers.boundedElastic());
	}

	private void invalidateSync() {
		Session<Void> session = this.session;
		try (BatchContext<B> context = this.manager.getBatcher().resumeBatch(this.batch)) {
			try (Batch batch = context.getBatch()) {
				session.invalidate();
			}
		}
	}

	@Override
	public void close() {
		try (BatchContext<B> context = this.manager.getBatcher().resumeBatch(this.batch)) {
			try (Batch batch = context.getBatch()) {
				try (Session<Void> session = this.session) {
					if (session.isValid()) {
						if (this.started) {
							// According to §7.6 of the servlet specification:
							// The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
							session.getMetaData().setLastAccess(this.startTime, Instant.now());
						} else {
							// Invalidate if session was never "started".
							session.invalidate();
						}
					}
				}
			}
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
}
