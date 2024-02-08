/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.authentication;

import org.wildfly.clustering.spring.session.SessionMarshallerFactory;
import org.wildfly.clustering.spring.session.SessionPersistenceGranularity;
import org.wildfly.clustering.spring.session.annotation.Indexing;
import org.wildfly.clustering.spring.session.annotation.SessionManager;
import org.wildfly.clustering.spring.session.infinispan.embedded.annotation.EnableInfinispanIndexedHttpSession;
import org.wildfly.clustering.spring.session.infinispan.embedded.annotation.Infinispan;

/**
 * Test configuration for session manager.
 * @author Paul Ferraro
 */
@EnableInfinispanIndexedHttpSession(config = @Infinispan, manager = @SessionManager(marshallerFactory = SessionMarshallerFactory.JBOSS, granularity = SessionPersistenceGranularity.ATTRIBUTE, maxActiveSessions = 100), indexing = @Indexing)
public class Config {
}
