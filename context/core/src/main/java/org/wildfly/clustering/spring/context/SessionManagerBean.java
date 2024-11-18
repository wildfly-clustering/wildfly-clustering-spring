/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.springframework.beans.factory.InitializingBean;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionStatistics;

/**
 * @author Paul Ferraro
 */
public class SessionManagerBean extends AutoDestroyBean implements SessionManager<Void>, InitializingBean {

	private final SessionManager<Void> manager;

	public SessionManagerBean(SessionManager<Void> manager) {
		this.manager = manager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
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
