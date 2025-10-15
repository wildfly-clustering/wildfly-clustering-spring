/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session;

import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;

/**
 * A mutable indexing configuration.
 * @author Paul Ferraro
 */
public interface MutableIndexingConfiguration extends IndexingConfiguration {
	/**
	 * Specifies the session indexes.
	 * @param indexes the session indexes.
	 */
	void setIndexes(Map<String, String> indexes);

	/**
	 * Specifies the index resolver for a session.
	 * @param resolver the index resolver for a session.
	 */
	void setIndexResolver(IndexResolver<Session> resolver);
}
