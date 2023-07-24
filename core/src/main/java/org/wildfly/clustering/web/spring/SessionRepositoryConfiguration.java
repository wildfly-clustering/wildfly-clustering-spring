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

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;

/**
 * Configuration for a session repository.
 * @author Paul Ferraro
 */
public interface SessionRepositoryConfiguration {

	Integer getMaxActiveSessions();
	SessionAttributePersistenceStrategy getPersistenceStrategy();
	Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory();
	Supplier<String> getIdentifierFactory();
	ApplicationEventPublisher getEventPublisher();
	ServletContext getServletContext();
	Map<String, String> getIndexes();
	IndexResolver<Session> getIndexResolver();
}
