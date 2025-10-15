/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.session.user.UserManager;

/**
 * Encapsulates the user configuration.
 * @author Paul Ferraro
 */
public interface UserConfiguration {
	/**
	 * Returns the user managers per index.
	 * @return the user managers per index.
	 */
	Map<String, UserManager<Void, Void, String, String>> getUserManagers();

	/**
	 * Returns the index resolver for a session.
	 * @return the index resolver for a session.
	 */
	IndexResolver<Session> getIndexResolver();
}
