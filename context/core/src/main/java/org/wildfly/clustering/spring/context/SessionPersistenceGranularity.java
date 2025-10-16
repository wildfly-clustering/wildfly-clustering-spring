/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context;

import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;

/**
 * Enumerates the default set of available attribute persistence strategies.
 * @author Paul Ferraro
 */
public enum SessionPersistenceGranularity implements Supplier<SessionAttributePersistenceStrategy> {
	/** Always persists all session attributes, preserving cross attribute object references */
	SESSION(SessionAttributePersistenceStrategy.COARSE),
	/** Persists modified/mutable session attributes only, any cross attribute object references will be lost */
	ATTRIBUTE(SessionAttributePersistenceStrategy.FINE),
	;
	private final SessionAttributePersistenceStrategy strategy;

	SessionPersistenceGranularity(SessionAttributePersistenceStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public SessionAttributePersistenceStrategy get() {
		return this.strategy;
	}
}
