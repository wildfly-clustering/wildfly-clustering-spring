/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.Map;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.spring.session.DistributableSessionRepository;
import org.wildfly.clustering.spring.session.DistributableSessionRepositoryConfiguration;
import org.wildfly.clustering.spring.session.SpringSession;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionRepository extends AbstractInfinispanSessionRepository implements FindByIndexNameSessionRepository<SpringSession> {

	private FindByIndexNameSessionRepository<SpringSession> repository;

	public InfinispanSessionRepository(InfinispanSessionRepositoryConfiguration configuration) {
		super(configuration);
	}

	@Override
	public void accept(DistributableSessionRepositoryConfiguration<TransactionBatch> configuration) {
		this.repository = new DistributableSessionRepository<>(configuration);
	}

	@Override
	public SpringSession createSession() {
		return this.repository.createSession();
	}

	@Override
	public SpringSession findById(String id) {
		return this.repository.findById(id);
	}

	@Override
	public void deleteById(String id) {
		this.repository.deleteById(id);
	}

	@Override
	public void save(SpringSession session) {
		this.repository.save(session);
	}

	@Override
	public Map<String, SpringSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
		return this.repository.findByIndexNameAndIndexValue(indexName, indexValue);
	}
}
