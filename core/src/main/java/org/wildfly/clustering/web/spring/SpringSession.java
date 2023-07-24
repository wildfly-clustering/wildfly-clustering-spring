/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.spring;

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
