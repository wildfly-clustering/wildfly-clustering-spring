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

package org.wildfly.clustering.web.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.wildfly.clustering.web.spring.SessionMarshallerFactory;
import org.wildfly.clustering.web.spring.SessionPersistenceGranularity;

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
