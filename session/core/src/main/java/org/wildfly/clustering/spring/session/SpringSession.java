/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import org.springframework.session.Session;

/**
 * A session with with an explicit lifecycle.
 * @author Paul Ferraro
 */
public interface SpringSession extends Session, AutoCloseable {

	/**
	 * Indicates whether this session was created during the current request.
	 * @return true, if this session was newly created, false otherwise.
	 */
	boolean isNew();

	/**
	 * To be invoked by {@link org.springframework.session.SessionRepository#save(Session)}.
	 */
	@Override
	void close();
}
