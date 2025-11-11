/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * A Spring Session repository facade for a {@link SessionManager}.
 * Additionally indexes sessions using a set of {@link UserManager} instances.
 * @author Paul Ferraro
 */
public class DistributableSessionRepository implements FindByIndexNameSessionRepository<SpringSession>, DisposableBean {
	private static final System.Logger LOGGER = System.getLogger(DistributableSessionRepository.class.getPackageName());
	// Handle redundant calls to findById(...)
	private static final ThreadLocal<SpringSession> CURRENT_SESSION = new ThreadLocal<>();

	private final SessionManager<Void> manager;
	private final ApplicationEventPublisher publisher;
	private final BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction;
	private final UserConfiguration indexing;
	private final StampedLock lifecycleLock = new StampedLock();

	/**
	 * Create a session repository from the specified configuration.
	 * @param configuration a session repository configuration
	 */
	public DistributableSessionRepository(DistributableSessionRepositoryConfiguration configuration) {
		this.manager = configuration.getSessionManager();
		this.publisher = configuration.getEventPublisher();
		this.destroyAction = configuration.getSessionDestroyAction();
		this.indexing = configuration.getUserConfiguration();
	}

	@Override
	public void destroy() {
		try {
			this.lifecycleLock.writeLockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public SpringSession createSession() {
		DistributableSession session = this.getSession(this.manager.getIdentifierFactory().thenApply(this.manager::createSession));
		this.publisher.publishEvent(new SessionCreatedEvent(this, session));
		CURRENT_SESSION.set(session);
		return session;
	}

	@Override
	public SpringSession findById(String id) {
		// Spring session lifecycle is a mess - and may call findById(...) multiple times per request
		// Handle redundant calls to findById(...) via thread local
		SpringSession current = CURRENT_SESSION.get();
		if ((current != null) && current.getId().equals(id)) {
			return current;
		}
		DistributableSession session = this.getSession(Supplier.of(id).thenApply(this.manager::findSession));
		if (session != null) {
			CURRENT_SESSION.set(session);
		}
		return session;
	}

	private DistributableSession getSession(Supplier<Session<Void>> factory) {
		Map.Entry<SuspendedBatch, Runnable> entry = this.createBatchEntry();
		SuspendedBatch suspendedBatch = entry.getKey();
		Runnable closeTask = entry.getValue();
		try (Context<Batch> context = suspendedBatch.resumeWithContext()) {
			Session<Void> session = factory.get();
			if ((session == null) || !session.isValid()) {
				return close(context, Consumer.empty(), closeTask);
			}
			return new DistributableSession(this.manager, session, suspendedBatch, closeTask, this.indexing, this.destroyAction);
		} catch (RuntimeException | Error e) {
			try (Context<Batch> context = suspendedBatch.resumeWithContext()) {
				close(context, Batch::discard, closeTask);
			}
			throw e;
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
		// Spring session lifecycle is a mess - and may save session multiple times per request
		// Ideally we would only close session on response commit - but SessionSessionRepository lacks that context
		if (CURRENT_SESSION.get() != null) {
			try {
				session.close();
			} finally {
				CURRENT_SESSION.remove();
			}
		}
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

	private Map.Entry<SuspendedBatch, Runnable> createBatchEntry() {
		Runnable closeTask = this.getSessionCloseTask();
		try {
			return Map.entry(this.manager.getBatchFactory().get().suspend(), closeTask);
		} catch (RuntimeException | Error e) {
			closeTask.run();
			throw e;
		}
	}

	private static DistributableSession close(java.util.function.Supplier<Batch> batchProvider, Consumer<Batch> batchTask, Runnable closeTask) {
		try (Batch batch = batchProvider.get()) {
			batchTask.accept(batch);
		} catch (RuntimeException | Error e) {
			LOGGER.log(System.Logger.Level.WARNING, e.getLocalizedMessage(), e);
		} finally {
			closeTask.run();
		}
		return null;
	}

	private Runnable getSessionCloseTask() {
		StampedLock lock = this.lifecycleLock;
		long stamp = lock.tryReadLock();
		if (!StampedLock.isReadLockStamp(stamp)) {
			throw new IllegalStateException();
		}
		AtomicLong stampRef = new AtomicLong(stamp);
		return new Runnable() {
			@Override
			public void run() {
				// Ensure we only unlock once.
				long stamp = stampRef.getAndSet(0L);
				if (StampedLock.isReadLockStamp(stamp)) {
					lock.unlockRead(stamp);
				}
			}
		};
	}
}
