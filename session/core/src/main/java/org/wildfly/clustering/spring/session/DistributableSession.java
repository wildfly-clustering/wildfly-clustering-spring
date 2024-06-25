/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * Spring Session implementation that delegates to a {@link Session} instance.
 * @author Paul Ferraro
 */
public class DistributableSession implements SpringSession {

	private final SessionManager<Void> manager;
	private final SuspendedBatch batch;
	private final Instant startTime;
	private final UserConfiguration indexing;
	private final BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	private volatile Session<Void> session;

	public DistributableSession(SessionManager<Void> manager, Session<Void> session, SuspendedBatch batch, UserConfiguration indexing, BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction) {
		this.manager = manager;
		this.session = session;
		this.batch = batch;
		this.indexing = indexing;
		this.destroyAction = destroyAction;
		this.startTime = session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
	}

	@Override
	public String changeSessionId() {
		Session<Void> oldSession = this.session;
		String id = this.manager.getIdentifierFactory().get();
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

		// Update indexes
		Map<String, String> indexes = this.indexing.getIndexResolver().resolveIndexesFor(this);
		for (Map.Entry<String, String> entry : indexes.entrySet()) {
			UserManager<Void, Void, String, String> manager = this.indexing.getUserManagers().get(entry.getKey());
			if (manager != null) {
				try (Batch batch = manager.getBatchFactory().get()) {
					User<Void, Void, String, String> sso = manager.findUser(entry.getValue());
					if (sso != null) {
						sso.getSessions().removeSession(oldSession.getId());
						sso.getSessions().addSession(id, id);
					}
				}
			}
		}

		return id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAttribute(String name) {
		return (T) this.session.getAttributes().get(name);
	}

	@Override
	public Set<String> getAttributeNames() {
		return this.session.getAttributes().keySet();
	}

	@Override
	public Instant getCreationTime() {
		return this.session.getMetaData().getCreationTime();
	}

	@Override
	public String getId() {
		return this.session.getId();
	}

	@Override
	public Instant getLastAccessedTime() {
		return this.session.getMetaData().getLastAccessTime();
	}

	@Override
	public Duration getMaxInactiveInterval() {
		return this.session.getMetaData().getTimeout();
	}

	@Override
	public boolean isExpired() {
		return this.session.getMetaData().isExpired();
	}

	@Override
	public void removeAttribute(String name) {
		this.setAttribute(name, null);
	}

	@Override
	public void setAttribute(String name, Object value) {
		Map<String, String> oldIndexes = this.indexing.getIndexResolver().resolveIndexesFor(this);

		this.session.getAttributes().put(name, value);

		// N.B. org.springframework.session.web.http.HttpSessionAdapter already triggers HttpSessionBindingListener events
		// However, Spring Session violates the servlet specification by not triggering HttpSessionAttributeListener events

		// Update indexes
		Map<String, String> indexes = this.indexing.getIndexResolver().resolveIndexesFor(this);
		if (!oldIndexes.isEmpty() || !indexes.isEmpty()) {
			Set<String> indexNames = new TreeSet<>();
			indexNames.addAll(oldIndexes.keySet());
			indexNames.addAll(indexes.keySet());
			for (String indexName : indexNames) {
				String oldIndexValue = oldIndexes.get(indexName);
				String indexValue = indexes.get(indexName);
				if (!Objects.equals(indexValue, oldIndexValue)) {
					UserManager<Void, Void, String, String> manager = this.indexing.getUserManagers().get(indexName);
					try (Batch batch = manager.getBatchFactory().get()) {
						if (oldIndexValue != null) {
							User<Void, Void, String, String> sso = manager.findUser(oldIndexValue);
							if (sso != null) {
								sso.invalidate();
							}
						}
						if (indexValue != null) {
							String sessionId = this.session.getId();
							User<Void, Void, String, String> sso = manager.createUser(indexValue, null);
							sso.getSessions().addSession(sessionId, sessionId);
						}
					}
				}
			}
		}
	}

	@Override
	public void setLastAccessedTime(Instant instant) {
		// We've already captured this
	}

	@Override
	public void setMaxInactiveInterval(Duration duration) {
		this.session.getMetaData().setTimeout(duration);
	}

	@Override
	public boolean isNew() {
		return this.session.getMetaData().isNew();
	}

	@Override
	public void invalidate() {
		try (Batch batch = this.batch.resume()) {
			this.destroyAction.accept(this.session, SessionDestroyedEvent::new);
			this.session.invalidate();
		}
	}

	@Override
	public void close() {
		// Spring session lifecycle logic is a mess.  Ensure we only close a session once.
		if (this.closed.compareAndSet(false, true)) {
			try (Batch batch = this.batch.resume()) {
				try (Session<Void> session = this.session) {
					if (session.isValid()) {
						// According to §7.6 of the servlet specification:
						// The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
						session.getMetaData().setLastAccess(this.startTime, Instant.now());
					}
				}
			}
		}
	}
}
