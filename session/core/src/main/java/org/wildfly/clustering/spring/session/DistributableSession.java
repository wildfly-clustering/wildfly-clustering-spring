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

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * Spring Session implementation that delegates to a {@link Session} instance.
 * @author Paul Ferraro
 */
public class DistributableSession<B extends Batch> implements SpringSession {

	private final SessionManager<Void, B> manager;
	private final B batch;
	private final Instant startTime;
	private final UserConfiguration<B> indexing;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	private volatile Session<Void> session;

	public DistributableSession(SessionManager<Void, B> manager, Session<Void> session, B batch, UserConfiguration<B> indexing) {
		this.manager = manager;
		this.session = session;
		this.batch = batch;
		this.indexing = indexing;
		this.startTime = session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
	}

	@Override
	public String changeSessionId() {
		Session<Void> oldSession = this.session;
		String id = this.manager.getIdentifierFactory().get();
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
				newSession.invalidate();
				throw e;
			}
		}

		// Update indexes
		Map<String, String> indexes = this.indexing.getIndexResolver().resolveIndexesFor(this);
		for (Map.Entry<String, String> entry : indexes.entrySet()) {
			UserManager<Void, Void, String, String, B> manager = this.indexing.getUserManagers().get(entry.getKey());
			if (manager != null) {
				try (B batch = manager.getBatcher().createBatch()) {
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
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return (T) session.getAttributes().getAttribute(name);
		}
	}

	@Override
	public Set<String> getAttributeNames() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return session.getAttributes().getAttributeNames();
		}
	}

	@Override
	public Instant getCreationTime() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return session.getMetaData().getCreationTime();
		}
	}

	@Override
	public String getId() {
		return this.session.getId();
	}

	@Override
	public Instant getLastAccessedTime() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return session.getMetaData().getLastAccessStartTime();
		}
	}

	@Override
	public Duration getMaxInactiveInterval() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return session.getMetaData().getTimeout();
		}
	}

	@Override
	public boolean isExpired() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return session.getMetaData().isExpired();
		}
	}

	@Override
	public void removeAttribute(String name) {
		this.setAttribute(name, null);
	}

	@Override
	public void setAttribute(String name, Object value) {
		Map<String, String> oldIndexes = this.indexing.getIndexResolver().resolveIndexesFor(this);

		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			session.getAttributes().setAttribute(name, value);
		}

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
					UserManager<Void, Void, String, String, B> manager = this.indexing.getUserManagers().get(indexName);
					try (B batch = manager.getBatcher().createBatch()) {
						if (oldIndexValue != null) {
							User<Void, Void, String, String> sso = manager.findUser(oldIndexValue);
							if (sso != null) {
								sso.invalidate();
							}
						}
						if (indexValue != null) {
							String sessionId = session.getId();
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
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			session.getMetaData().setTimeout(duration);
		}
	}

	@Override
	public boolean isNew() {
		Session<Void> session = this.session;
		try (BatchContext context = this.resumeBatch()) {
			return session.getMetaData().isNew();
		}
	}

	@Override
	public void close() {
		// Spring session lifecycle logic is a mess.  Ensure we only close a session once.
		if (this.closed.compareAndSet(false, true)) {
			Session<Void> requestSession = this.session;
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
			}
		}
	}

	private BatchContext resumeBatch() {
		B batch = (this.batch != null) && (this.batch.getState() != Batch.State.CLOSED) ? this.batch : null;
		return this.manager.getBatcher().resumeBatch(batch);
	}
}
