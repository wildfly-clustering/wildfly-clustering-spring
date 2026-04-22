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
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.function.BiConsumer;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.server.util.BlockingReference;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * A Spring Session facade for an distributable session.
 * @author Paul Ferraro
 */
public class DistributableSession extends DistributableImmutableSession<Session<Void>> {

	private final SessionManager<Void> manager;
	private final BlockingReference<Session<Void>> reference;
	private final Instant startTime;
	private final UserConfiguration configuration;
	private final BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction;
	private final AtomicReference<Runnable> closeTask;

	/**
	 * Creates a Spring Session facade for an distributable session.
	 * @param manager the associated session manager
	 * @param session the distributable session
	 * @param closeTask a task to invoke on session close
	 * @param configuration a user configuration
	 * @param destroyAction an action to perform on session destroy
	 */
	public DistributableSession(SessionManager<Void> manager, Session<Void> session, Runnable closeTask, UserConfiguration configuration, BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction) {
		this(manager, BlockingReference.of(session), closeTask, configuration, destroyAction, session.getMetaData().getLastAccessTime().isEmpty() ? session.getMetaData().getCreationTime() : Instant.now());
	}

	private DistributableSession(SessionManager<Void> manager, BlockingReference<Session<Void>> reference, Runnable closeTask, UserConfiguration configuration, BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction, Instant startTime) {
		super(reference);
		this.manager = manager;
		this.reference = reference;
		this.closeTask = new AtomicReference<>(closeTask);
		this.configuration = configuration;
		this.destroyAction = destroyAction;
		this.startTime = startTime;
	}

	@Override
	public String changeSessionId() {
		String newId = this.manager.getIdentifierFactory().get();
		AtomicReference<String> oldId = new AtomicReference<>();
		String currentId = this.reference.getWriter(Session::isValid).updateAndGet(currentSession -> {
			oldId.setPlain(currentSession.getId());
			SessionMetaData currentMetaData = currentSession.getMetaData();
			Map<String, Object> currentAttributes = currentSession.getAttributes();
			Session<Void> newSession = this.manager.createSession(newId);
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
		}).getId();
		if (currentId.equals(newId)) {
			// Update indexes
			Map<String, String> indexes = this.configuration.getIndexResolver().resolveIndexesFor(this);
			for (Map.Entry<String, String> entry : indexes.entrySet()) {
				UserManager<Void, Void, String, String> manager = this.configuration.getUserManagers().get(entry.getKey());
				if (manager != null) {
					try (Batch batch = manager.getBatchFactory().get()) {
						try (User<Void, Void, String, String> sso = manager.findUser(entry.getValue())) {
							if (sso != null) {
								sso.getSessions().removeSession(oldId.getPlain());
								sso.getSessions().addSession(currentId, currentId);
							}
						}
					}
				}
			}
		}
		return currentId;
	}

	@Override
	public void removeAttribute(String name) {
		this.setAttribute(name, null);
	}

	@Override
	public void setAttribute(String name, Object value) {
		this.reference.getReader().map(Session.REQUIRE_VALID).read(session -> {
			Map<String, String> oldIndexes = this.configuration.getIndexResolver().resolveIndexesFor(this);

			session.getAttributes().put(name, value);

			// N.B. org.springframework.session.web.http.HttpSessionAdapter already triggers HttpSessionBindingListener events
			// However, Spring Session violates the servlet specification by not triggering HttpSessionAttributeListener events

			// Update indexes
			Map<String, String> indexes = this.configuration.getIndexResolver().resolveIndexesFor(this);
			if (!oldIndexes.isEmpty() || !indexes.isEmpty()) {
				Set<String> indexNames = new TreeSet<>();
				indexNames.addAll(oldIndexes.keySet());
				indexNames.addAll(indexes.keySet());
				for (String indexName : indexNames) {
					String oldIndexValue = oldIndexes.get(indexName);
					String indexValue = indexes.get(indexName);
					if (!Objects.equals(indexValue, oldIndexValue)) {
						UserManager<Void, Void, String, String> manager = this.configuration.getUserManagers().get(indexName);
						try (Batch batch = manager.getBatchFactory().get()) {
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
		});
	}

	@Override
	public void setLastAccessedTime(Instant instant) {
		// We already captured this in the constructor
	}

	@Override
	public void setMaxInactiveInterval(Duration duration) {
		this.reference.getReader().map(Session.METADATA).read(SessionMetaData.MAX_IDLE.composeUnary(Function.identity(), Function.of(duration)));
	}

	@Override
	public void invalidate() {
		// Spring does not call SessionRepository.save(...) for invalid sessions
		// Therefore, we must close the session here.
		Runnable closeTask = this.closeTask.getAndSet(null);
		try {
			this.reference.getReader().map(Session.REQUIRE_VALID).read(invalidSession -> {
				try (Session<?> session = invalidSession) {
					this.destroyAction.accept(session, SessionDestroyedEvent::new);
					session.invalidate();
				}
			});
		} finally {
			if (closeTask != null) {
				closeTask.run();
			}
		}
	}

	@Override
	public void close() {
		// Spring session lifecycle logic is a mess.  Ensure we only close a session once.
		Runnable closeTask = this.closeTask.getAndSet(null);
		if (closeTask != null) {
			try {
				this.reference.getReader().read(completeSession -> {
					try (Session<Void> session = completeSession) {
						if (session.isValid()) {
							// According to §7.6 of the servlet specification:
							// The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
							session.getMetaData().setLastAccess(this.startTime, Instant.now());
						}
					}
				});
			} finally {
				closeTask.run();
			}
		}
	}
}
