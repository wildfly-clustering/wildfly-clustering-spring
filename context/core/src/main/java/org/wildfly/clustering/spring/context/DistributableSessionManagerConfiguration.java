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
 * @author Paul Ferraro
 * @param <S> session type
 * @param <B> batch type
 */
public interface DistributableSessionManagerConfiguration<S, B extends Batch> {
	SessionManager<Void> getSessionManager();
	ApplicationEventPublisher getEventPublisher();
	BiConsumer<ImmutableSession, BiFunction<Object, S, ApplicationEvent>> getSessionDestroyAction();
}
