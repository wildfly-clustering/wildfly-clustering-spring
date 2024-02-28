/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.session.OOBSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionAttributes;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Paul Ferraro
 */
public class DistributableWebSession<B extends Batch> implements SpringWebSession, Map<String, Object> {

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
		return this;
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
			try (BatchContext context = this.resumeBatch()) {
				Session<Void> newSession = this.manager.createSession(id);
				try {
					for (String name: oldSession.getAttributes().getAttributeNames()) {
						newSession.getAttributes().setAttribute(name, oldSession.getAttributes().getAttribute(name));
					}
					newSession.getMetaData().setTimeout(oldSession.getMetaData().getTimeout());
					newSession.getMetaData().setLastAccess(oldSession.getMetaData().getLastAccessStartTime(), oldSession.getMetaData().getLastAccessTime());
					oldSession.invalidate();
					this.session = newSession;
				} catch (IllegalStateException e) {
					if (!oldSession.isValid()) {
						oldSession.close();
					}
					newSession.invalidate();
					throw e;
				}
			}
		});
	}

	@Override
	public Mono<Void> invalidate() {
		return Mono.<Void>fromRunnable(this::invalidateSync).publishOn(Schedulers.boundedElastic());
	}

	private void invalidateSync() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			session.invalidate();
			if (this.batch != null) {
				this.batch.close();
			}
		} catch (IllegalStateException e) {
			if (!session.isValid()) {
				session.close();
			}
			throw e;
		}
	}

	@Override
	public void close() {
		Session<Void> requestSession = this.session;
		if (this.started) {
			try (BatchContext context = this.resumeBatch()) {
				// If batch was discarded, close it
				if (this.batch.getState() == Batch.State.DISCARDED) {
					this.batch.close();
				}
				// If batch is closed, close valid session in a new batch
				try (Batch batch = (this.batch.getState() == Batch.State.CLOSED) && requestSession.isValid() ? this.manager.getBatcher().createBatch() : this.batch) {
					// Ensure session is closed, even if invalid
					try (Session<Void> session = requestSession) {
						if (session.isValid()) {
							// According to §7.6 of the servlet specification:
							// The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
							session.getMetaData().setLastAccess(this.startTime, Instant.now());
						}
					}
				}
			} finally {
				// Switch to OOB session, in case this session is referenced outside the scope of this request
				this.session = new OOBSession<>(this.manager, requestSession.getId(), null);
			}
		} else if (requestSession.isValid()) {
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
		return this.applyMetaData(SessionMetaData::isExpired);
	}

	@Override
	public Instant getCreationTime() {
		return this.applyMetaData(SessionMetaData::getCreationTime);
	}

	@Override
	public Instant getLastAccessTime() {
		return this.applyMetaData(SessionMetaData::getLastAccessTime);
	}

	@Override
	public void setMaxIdleTime(Duration maxIdleTime) {
		this.accept(session -> session.getMetaData().setTimeout(maxIdleTime));
	}

	@Override
	public Duration getMaxIdleTime() {
		return this.applyMetaData(SessionMetaData::getTimeout);
	}

	@Override
	public Set<String> keySet() {
		return this.applyAttributes(SessionAttributes::getAttributeNames);
	}

	@Override
	public Object get(Object object) {
		if (!(object instanceof String key)) {
			throw (object != null) ? new IllegalArgumentException(object.toString()) : new IllegalArgumentException();
		}
		return this.applyAttributes(attributes -> attributes.getAttribute(key));
	}

	@Override
	public Object put(String key, Object value) {
		this.started = true;
		return this.applyAttributes(attributes -> attributes.setAttribute(key, value));
	}

	@Override
	public Object remove(Object object) {
		if (!(object instanceof String key)) {
			throw (object != null) ? new IllegalArgumentException(object.toString()) : new IllegalArgumentException();
		}
		return this.applyAttributes(attributes -> attributes.removeAttribute(key));
	}

	@Override
	public int size() {
		return this.keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return this.keySet().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.keySet().contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.applyAttributes(attributes -> attributes.getAttributeNames().stream().map(attributes::getAttribute).anyMatch(value::equals));
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		this.started = true;
		this.accept(session -> map.entrySet().forEach(entry -> session.getAttributes().setAttribute(entry.getKey(), entry.getValue())));
	}

	@Override
	public void clear() {
		this.accept(session -> session.getAttributes().getAttributeNames().forEach(session.getAttributes()::removeAttribute));
	}

	@Override
	public Collection<Object> values() {
		return this.applyAttributes(attributes -> attributes.getAttributeNames().stream().map(attributes::getAttribute).collect(Collectors.toUnmodifiableList()));
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		return this.applyAttributes(attributes -> attributes.getAttributeNames().stream().collect(Collectors.toUnmodifiableMap(Function.identity(), attributes::getAttribute)).entrySet());
	}

	private <R> R applyAttributes(Function<SessionAttributes, R> function) {
		return this.apply(function.compose(Session::getAttributes));
	}

	private <R> R applyMetaData(Function<SessionMetaData, R> function) {
		return this.apply(function.compose(Session::getMetaData));
	}

	private <R> R apply(Function<Session<Void>, R> function) {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return function.apply(session);
		} catch (IllegalStateException e) {
			if (!session.isValid()) {
				session.close();
			}
			throw e;
		}
	}

	private void accept(Consumer<Session<Void>> consumer) {
		this.apply(session -> {
			consumer.accept(session);
			return null;
		});
	}

	private BatchContext resumeBatch() {
		B batch = (this.batch != null) && (this.batch.getState() != Batch.State.CLOSED) ? this.batch : null;
		return this.manager.getBatcher().resumeBatch(batch);
	}
}
