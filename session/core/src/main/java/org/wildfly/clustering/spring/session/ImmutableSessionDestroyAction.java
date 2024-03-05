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
import org.wildfly.clustering.session.spec.SessionSpecificationProvider;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionDestroyAction<B extends Batch> implements BiConsumer<ImmutableSession, BiFunction<Object, Session, ApplicationEvent>> {

	private final ApplicationEventPublisher publisher;
	private final ServletContext context;
	private final SessionSpecificationProvider<HttpSession, ServletContext> provider;
	private final UserConfiguration<B> indexing;

	public ImmutableSessionDestroyAction(ApplicationEventPublisher publisher, ServletContext context, SessionSpecificationProvider<HttpSession, ServletContext> provider, UserConfiguration<B> indexing) {
		this.publisher = publisher;
		this.context = context;
		this.provider = provider;
		this.indexing = indexing;
	}

	@Override
	public void accept(ImmutableSession session, BiFunction<Object, Session, ApplicationEvent> eventFactory) {
		ApplicationEvent event = eventFactory.apply(this, new DistributableImmutableSession(session));
		this.publisher.publishEvent(event);
		HttpSession httpSession = this.provider.asSession(session, this.context);
		for (Map.Entry<String, Object> entry : session.getAttributes().entrySet()) {
			if (entry.getValue() instanceof HttpSessionBindingListener listener) {
				try {
					listener.valueUnbound(new HttpSessionBindingEvent(httpSession, entry.getKey(), listener));
				} catch (Throwable e) {
					this.context.log(e.getMessage(), e);
				}
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
