/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import org.springframework.web.server.session.WebSessionIdResolver;
import org.wildfly.clustering.session.SessionManager;

/**
 * Encapsulates the configuration of a Spring Web session manager.
 * @author Paul Ferraro
 */
public interface DistributableWebSessionManagerConfiguration {
	/**
	 * Returns the distributed session manager.
	 * @return the distributed session manager.
	 */
	SessionManager<Void> getSessionManager();

	/**
	 * Returns the session identifier resolver.
	 * @return the session identifier resolver.
	 */
	WebSessionIdResolver getSessionIdentifierResolver();
}
