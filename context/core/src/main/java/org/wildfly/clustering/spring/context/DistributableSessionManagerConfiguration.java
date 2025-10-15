/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;

/**
 * Encapsulates the configuration of a session manager.
 * @author Paul Ferraro
 * @param <S> the session type
 * @param <B> the batch type
 */
public interface DistributableSessionManagerConfiguration<S, B extends Batch> {
	/**
	 * Returns the associated session manager.
	 * @return the associated session manager.
	 */
	SessionManager<Void> getSessionManager();

	/**
	 * Returns the associated event publisher.
	 * @return the associated event publisher.
	 */
	ApplicationEventPublisher getEventPublisher();

	/**
	 * Returns a consumer that accepts a session to be destroyed.
	 * @return a consumer that accepts a session to be destroyed.
	 */
	BiConsumer<ImmutableSession, BiFunction<Object, S, ApplicationEvent>> getSessionDestroyAction();
}
