/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.context;

import org.springframework.context.annotation.PropertySource;
import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionPersistenceGranularity;
import org.wildfly.clustering.spring.context.config.annotation.SessionManager;
import org.wildfly.clustering.spring.context.infinispan.embedded.config.annotation.Infinispan;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.annotation.EnableInfinispanHttpSession;

/**
 * Test configuration for session manager.
 * @author Paul Ferraro
 */
@PropertySource("classpath:application.properties")
@EnableInfinispanHttpSession(config = @Infinispan, manager = @SessionManager(marshaller = SessionAttributeMarshaller.PROTOSTREAM, granularity = SessionPersistenceGranularity.ATTRIBUTE, maxActiveSessions = 100))
public class Config {
}
