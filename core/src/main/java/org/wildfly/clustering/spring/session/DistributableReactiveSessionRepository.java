/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;

import reactor.core.publisher.Mono;

/**
 * A reactive session repository implementation based on a {@link SessionManager}.
 * @author Paul Ferraro
 */
public class DistributableReactiveSessionRepository<B extends Batch> implements ReactiveSessionRepository<SpringSession> {
	private final SessionManager<Void, B> manager;
	private final ApplicationEventPublisher publisher;
	private final BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction;
	private final IndexingConfiguration<B> indexing;

	public DistributableReactiveSessionRepository(DistributableSessionRepositoryConfiguration<B> configuration) {
		this.manager = configuration.getSessionManager();
		this.publisher = configuration.getEventPublisher();
		this.destroyAction = configuration.getSessionDestroyAction();
		this.indexing = configuration.getIndexingConfiguration();
	}

	@Override
	public Mono<SpringSession> createSession() {
		String id = this.manager.getIdentifierFactory().get();
		return Mono.fromCompletionStage(this.manager.createSessionAsync(id).thenApply(session -> {
			SpringSession result = new DistributableSession<>(this.manager, session, null, this.indexing);
			this.publisher.publishEvent(new SessionCreatedEvent(this, result));
			return result;
		}));
	}

	@Override
	public Mono<SpringSession> findById(String id) {
		CompletionStage<SpringSession> future = this.manager.findSessionAsync(id).thenApply(session -> (session != null) ? new DistributableSession<>(this.manager, session, null, this.indexing) : null);
		return Mono.fromCompletionStage(future);
	}

	@Override
	public Mono<Void> deleteById(String id) {
		CompletionStage<Void> future = this.manager.findSessionAsync(id).thenAccept(session -> {
			if (session != null) {
				this.destroyAction.accept(session, SessionDestroyedEvent::new);
				session.invalidate();
			}
		});
		return Mono.fromCompletionStage(future);
	}

	@Override
	public Mono<Void> save(SpringSession session) {
		return Mono.fromRunnable(session::close);
	}
}
