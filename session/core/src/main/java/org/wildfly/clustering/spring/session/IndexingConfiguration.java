/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session;

import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;

/**
 * Encapsulates the indexing configuration of a session repository.
 * @author Paul Ferraro
 */
public interface IndexingConfiguration {
	/**
	 * Returns the indexes for a given session.
	 * @return the indexes for a given session.
	 */
	Map<String, String> getIndexes();

	/**
	 * Returns the index resolver for a session.
	 * @return the index resolver for a session.
	 */
	IndexResolver<Session> getIndexResolver();
}
