/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import org.springframework.web.server.session.WebSessionIdResolver;
import org.wildfly.clustering.session.SessionManager;

/**
 * @author Paul Ferraro
 */
public interface DistributableWebSessionManagerConfiguration {
	SessionManager<Void> getSessionManager();
	WebSessionIdResolver getSessionIdentifierResolver();
}
