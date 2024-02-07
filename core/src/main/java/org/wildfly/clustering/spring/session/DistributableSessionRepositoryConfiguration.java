/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;

/**
 * @author Paul Ferraro
 */
public interface DistributableSessionRepositoryConfiguration<B extends Batch> {
	SessionManager<Void, B> getSessionManager();
	ApplicationEventPublisher getEventPublisher();
	BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> getSessionDestroyAction();
	IndexingConfiguration<B> getIndexingConfiguration();
}
