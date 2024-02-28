/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionDestroyAction<B extends Batch> implements BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> {

	private final ApplicationEventPublisher publisher;
	private final ServletContext context;
	private final UserConfiguration<B> indexing;

	public ImmutableSessionDestroyAction(ApplicationEventPublisher publisher, ServletContext context, UserConfiguration<B> indexing) {
		this.publisher = publisher;
		this.context = context;
		this.indexing = indexing;
	}

	@Override
	public void accept(ImmutableSession session, BiFunction<Object, Session, ApplicationEvent> eventFactory) {
		ApplicationEvent event = eventFactory.apply(this, new DistributableImmutableSession(session));
		this.publisher.publishEvent(event);
		HttpSession httpSession = JakartaServletFacadeProvider.INSTANCE.asSession(session, this.context);
		for (Map.Entry<String, HttpSessionBindingListener> entry : session.getAttributes().getAttributes(HttpSessionBindingListener.class).entrySet()) {
			HttpSessionBindingListener listener = entry.getValue();
			try {
				listener.valueUnbound(new HttpSessionBindingEvent(httpSession, entry.getKey(), listener));
			} catch (Throwable e) {
				this.context.log(e.getMessage(), e);
			}
		}

		// Remove any associated indexes
		Map<String, String> indexes = this.indexing.getIndexResolver().resolveIndexesFor(new DistributableImmutableSession(session));
		for (Map.Entry<String, String> entry : indexes.entrySet()) {
			UserManager<Void, Void, String, String, B> manager = this.indexing.getUserManagers().get(entry.getKey());
			if (manager != null) {
				try (B batch = manager.getBatcher().createBatch()) {
					User<Void, Void, String, String> sso = manager.findUser(entry.getValue());
					if (sso != null) {
						sso.invalidate();
					}
				}
			}
		}
	}
}
