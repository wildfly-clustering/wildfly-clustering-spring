/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.concurrent.CompletionStage;

import org.springframework.beans.factory.InitializingBean;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionStatistics;

/**
 * A Spring bean decorator for a session manager.
 * @author Paul Ferraro
 */
public class SessionManagerBean extends AutoDestroyBean implements SessionManager<Void>, InitializingBean {

	private final SessionManager<Void> manager;

	/**
	 * Creates a new session manager bean.
	 * @param manager the decorated session manager.
	 */
	public SessionManagerBean(SessionManager<Void> manager) {
		this.manager = manager;
	}

	@Override
	public void afterPropertiesSet() {
		this.manager.start();
		this.accept(this.manager::stop);
	}

	@Override
	public Supplier<Batch> getBatchFactory() {
		return this.manager.getBatchFactory();
	}

	@Override
	public Supplier<String> getIdentifierFactory() {
		return this.manager.getIdentifierFactory();
	}

	@Override
	public void start() {
		this.manager.start();
	}

	@Override
	public void stop() {
		this.manager.stop();
	}

	@Override
	public boolean isStarted() {
		return this.manager.isStarted();
	}

	@Override
	public CompletionStage<Session<Void>> createSessionAsync(String id) {
		return this.manager.createSessionAsync(id);
	}

	@Override
	public CompletionStage<Session<Void>> findSessionAsync(String id) {
		return this.manager.findSessionAsync(id);
	}

	@Override
	public CompletionStage<ImmutableSession> findImmutableSessionAsync(String id) {
		return this.manager.findImmutableSessionAsync(id);
	}

	@Override
	public Session<Void> getDetachedSession(String id) {
		return this.manager.getDetachedSession(id);
	}

	@Override
	public SessionStatistics getStatistics() {
		return this.manager.getStatistics();
	}
}
