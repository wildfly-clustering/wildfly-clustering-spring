/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.Session;
import org.wildfly.clustering.function.BiConsumer;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;

/**
 * Encapsulates the configuration of a session repository.
 * @author Paul Ferraro
 */
public interface DistributableSessionRepositoryConfiguration {
	/**
	 * Returns the distributed session manager.
	 * @return the distributed session manager.
	 */
	SessionManager<Void> getSessionManager();

	/**
	 * Returns the application event publisher.
	 * @return the application event publisher.
	 */
	ApplicationEventPublisher getEventPublisher();

	/**
	 * Returns the action to perform on session destroy.
	 * @return the action to perform on session destroy.
	 */
	BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> getSessionDestroyAction();

	/**
	 * Returns the user configuration.
	 * @return the user configuration.
	 */
	UserConfiguration getUserConfiguration();
}
