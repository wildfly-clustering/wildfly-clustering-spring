/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.wildfly.clustering.spring.session.SessionMarshallerFactory;
import org.wildfly.clustering.spring.session.SessionPersistenceGranularity;

/**
 * Configures the session management characteristics of a session repository.
 * @author Paul Ferraro
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface SessionManager {
	/**
	 * Defines the marshaller implementation to use for session attribute marshalling.  Default uses JBoss Marshalling.
	 * @return a marshaller implementation
	 */
	SessionMarshallerFactory marshallerFactory() default SessionMarshallerFactory.JBOSS;

	/**
	 * Defines the granularity for persisting/replicating sessions, i.e. per session or per attribute.  Default is per-session.
	 * @return the session persistence granularity
	 */
	SessionPersistenceGranularity granularity() default SessionPersistenceGranularity.SESSION;

	/**
	 * The maximum number of sessions to retain in memory.  Default is limitless.
	 * @return the number of session to retain in memory.
	 */
	int maxActiveSessions() default -1;
}
