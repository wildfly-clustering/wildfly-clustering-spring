/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.springframework.beans.factory.InitializingBean;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.Batcher;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionStatistics;

/**
 * @author Paul Ferraro
 */
public class SessionManagerBean<B extends Batch> extends AutoDestroyBean implements SessionManager<Void, B>, InitializingBean {

	private final SessionManager<Void, B> manager;

	public SessionManagerBean(SessionManager<Void, B> manager) {
		this.manager = manager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.manager.start();
		this.accept(this.manager::stop);
	}

	@Override
	public Batcher<B> getBatcher() {
		return this.manager.getBatcher();
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
	public SessionStatistics getStatistics() {
		return this.manager.getStatistics();
	}
}
