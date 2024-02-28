/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import org.springframework.web.server.WebSession;

/**
 * @author Paul Ferraro
 */
public interface SpringWebSession extends WebSession, AutoCloseable {

	/**
	 * Indicates whether this session was created during the current request.
	 * @return true, if this session was newly created, false otherwise.
	 */
	boolean isNew();

	/**
	 * Indicates whether this session was invalidated by the current request.
	 * @return true, if this session was invalidated, false otherwise.
	 */
	boolean isValid();

	/**
	 * To be invoked by {@link org.springframework.web.server.WebSession#save()}.
	 */
	@Override
	void close();
}
