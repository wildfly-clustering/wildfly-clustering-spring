/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionPersistenceGranularity;

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
	SessionAttributeMarshaller marshaller() default SessionAttributeMarshaller.JBOSS;

	/**
	 * Defines the granularity for persisting/replicating sessions, i.e. per session or per attribute.  Default is per-session.
	 * @return the session persistence granularity
	 */
	SessionPersistenceGranularity granularity() default SessionPersistenceGranularity.SESSION;

	/**
	 * Defines the maximum number of sessions to retain in memory.
	 * Default is limitless.
	 * @return the maximum number of sessions to retain in memory.
	 */
	int maxActiveSessions() default Integer.MAX_VALUE;

	/**
	 * Defines the duration of time, expressed in ISO-8601 format, after which an idle session should passivate.
	 * @return the duration of time, expressed in ISO-8601 format, after which an idle session should passivate.
	 */
	String idleTimeout() default "PT0S";
}
