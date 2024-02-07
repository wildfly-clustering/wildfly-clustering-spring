/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote;

import org.springframework.session.ReactiveSessionRepository;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.spring.session.DistributableReactiveSessionRepository;
import org.wildfly.clustering.spring.session.DistributableSessionRepositoryConfiguration;
import org.wildfly.clustering.spring.session.SpringSession;

import reactor.core.publisher.Mono;

/**
 * @author Paul Ferraro
 */
public class HotRodReactiveSessionRepository extends AbstractHotRodSessionRepository implements ReactiveSessionRepository<SpringSession> {

	private ReactiveSessionRepository<SpringSession> repository;

	public HotRodReactiveSessionRepository(HotRodSessionRepositoryConfiguration configuration) {
		super(configuration);
	}

	@Override
	public void accept(DistributableSessionRepositoryConfiguration<TransactionBatch> configuration) {
		this.repository = new DistributableReactiveSessionRepository<>(configuration);
	}

	@Override
	public Mono<SpringSession> createSession() {
		return this.repository.createSession();
	}

	@Override
	public Mono<Void> save(SpringSession session) {
		return this.repository.save(session);
	}

	@Override
	public Mono<SpringSession> findById(String id) {
		return this.repository.findById(id);
	}

	@Override
	public Mono<Void> deleteById(String id) {
		return this.repository.deleteById(id);
	}
}
