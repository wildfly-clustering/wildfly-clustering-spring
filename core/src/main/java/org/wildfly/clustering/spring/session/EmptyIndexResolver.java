/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Collections;
import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;

/**
 * Resolver for a non-indexing session repository.
 * @author Paul Ferraro
 */
public enum EmptyIndexResolver implements IndexResolver<Session> {
	INSTANCE;

	@Override
	public Map<String, String> resolveIndexesFor(Session session) {
		return Collections.emptyMap();
	}
}
