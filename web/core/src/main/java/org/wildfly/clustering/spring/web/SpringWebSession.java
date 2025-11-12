/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import org.springframework.web.server.WebSession;

/**
 * A Spring web session with a lifecycle.
 * @author Paul Ferraro
 */
public interface SpringWebSession extends WebSession {

	/**
	 * Indicates whether this session was invalidated by the current request.
	 * @return true, if this session was invalidated, false otherwise.
	 */
	boolean isValid();
}
