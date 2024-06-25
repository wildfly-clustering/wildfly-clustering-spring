/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * A session repository implementation based on a {@link SessionManager}.
 * Additionally indexes sessions using a set of {@link UserManager} instances.
 * @author Paul Ferraro
 */
public class DistributableSessionRepository implements FindByIndexNameSessionRepository<SpringSession> {
	// Handle redundant calls to findById(...)
	private static final ThreadLocal<SpringSession> CURRENT_SESSION = new ThreadLocal<>();

	private final SessionManager<Void> manager;
	private final ApplicationEventPublisher publisher;
	private final BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction;
	private final UserConfiguration indexing;

	public DistributableSessionRepository(DistributableSessionRepositoryConfiguration configuration) {
		this.manager = configuration.getSessionManager();
		this.publisher = configuration.getEventPublisher();
		this.destroyAction = configuration.getSessionDestroyAction();
		this.indexing = configuration.getUserConfiguration();
	}

	@Override
	public SpringSession createSession() {
		String id = this.manager.getIdentifierFactory().get();
		boolean close = true;
		Batch batch = this.manager.getBatchFactory().get();
		try {
			Session<Void> session = this.manager.createSession(id);
			DistributableSession result = new DistributableSession(this.manager, session, batch.suspend(), this.indexing, this.destroyAction);
			this.publisher.publishEvent(new SessionCreatedEvent(this, result));
			close = false;
			return result;
		} catch (RuntimeException | Error e) {
			batch.discard();
			throw e;
		} finally {
			if (close) {
				batch.close();
			}
		}
	}

	@Override
	public SpringSession findById(String id) {
		// Handle redundant calls to findById(...)
		SpringSession current = CURRENT_SESSION.get();
		if ((current != null) && current.getId().equals(id)) {
			return current;
		}
		boolean close = true;
		Batch batch = this.manager.getBatchFactory().get();
		try {
			Session<Void> session = this.manager.findSession(id);
			if (session == null) return null;
			DistributableSession result = new DistributableSession(this.manager, session, batch.suspend(), this.indexing, this.destroyAction);
			close = false;
			CURRENT_SESSION.set(result);
			return result;
		} catch (RuntimeException | Error e) {
			batch.discard();
			throw e;
		} finally {
			if (close) {
				batch.close();
			}
		}
	}

	@Override
	public void deleteById(String id) {
		try (SpringSession session = this.findById(id)) {
			if (session != null) {
				session.invalidate();
			}
		} finally {
			CURRENT_SESSION.remove();
		}
	}

	@Override
	public void save(SpringSession session) {
		CURRENT_SESSION.remove();
		session.close();
	}

	@Override
	public Map<String, SpringSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
		Set<String> sessions = Collections.emptySet();
		UserManager<Void, Void, String, String> manager = this.indexing.getUserManagers().get(indexName);
		if (manager != null) {
			try (Batch batch = manager.getBatchFactory().get()) {
				User<Void, Void, String, String> sso = manager.findUser(indexValue);
				if (sso != null) {
					sessions = sso.getSessions().getDeployments();
				}
			}
		}
		if (!sessions.isEmpty()) {
			Map<String, SpringSession> result = new HashMap<>();
			try (Batch batch = manager.getBatchFactory().get()) {
				for (String sessionId : sessions) {
					ImmutableSession session = this.manager.findImmutableSession(sessionId);
					if (session != null) {
						result.put(sessionId, new DistributableImmutableSession(session));
					}
				}
			}
			return result;
		}
		return Collections.emptyMap();
	}
}
