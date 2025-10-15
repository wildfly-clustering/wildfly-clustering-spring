/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationEvent;
import org.springframework.session.Session;
import org.springframework.session.events.SessionExpiredEvent;
import org.wildfly.clustering.context.ContextualExecutor;
import org.wildfly.clustering.context.ThreadContextClassLoaderReference;
import org.wildfly.clustering.session.ImmutableSession;

/**
 * Executes a destroy action using the classloader of the servlet context.
 * @author Paul Ferraro
 */
public class ImmutableSessionExpirationListener implements Consumer<ImmutableSession> {

	private final BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> destroyAction;
	private final ContextualExecutor executor;

	/**
	 * Creates a session expiration listener.
	 * @param context the servlet context
	 * @param destroyAction the session destroy action
	 */
	public ImmutableSessionExpirationListener(ServletContext context, BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> destroyAction) {
		this.destroyAction = destroyAction;
		this.executor = ContextualExecutor.withContextProvider(ThreadContextClassLoaderReference.CURRENT.provide(context.getClassLoader()));
	}

	@Override
	public void accept(ImmutableSession session) {
		this.executor.execute(this.destroyAction, session, SessionExpiredEvent::new);
	}
}
