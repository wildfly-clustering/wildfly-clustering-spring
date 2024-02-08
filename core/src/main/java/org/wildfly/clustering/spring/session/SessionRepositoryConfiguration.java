/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;

/**
 * Configuration for a session repository.
 * @author Paul Ferraro
 */
public interface SessionRepositoryConfiguration {

	OptionalInt getMaxActiveSessions();
	SessionAttributePersistenceStrategy getPersistenceStrategy();
	Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory();
	Supplier<String> getIdentifierFactory();
	ApplicationEventPublisher getEventPublisher();
	ServletContext getServletContext();
	Map<String, String> getIndexes();
	IndexResolver<Session> getIndexResolver();
}
