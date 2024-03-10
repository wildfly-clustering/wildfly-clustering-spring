/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.infinispan.embedded.context;

import org.springframework.context.annotation.Configuration;
import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionPersistenceGranularity;
import org.wildfly.clustering.spring.context.config.annotation.SessionManager;
import org.wildfly.clustering.spring.context.infinispan.embedded.config.annotation.Infinispan;
import org.wildfly.clustering.spring.web.context.ReactiveConfig;
import org.wildfly.clustering.spring.web.infinispan.embedded.config.annotation.EnableInfinispanWebSession;

/**
 * Test configuration for session manager.
 * @author Paul Ferraro
 */
@EnableInfinispanWebSession(config = @Infinispan, manager = @SessionManager(marshaller = SessionAttributeMarshaller.PROTOSTREAM, granularity = SessionPersistenceGranularity.ATTRIBUTE, maxActiveSessions = 100))
@Configuration(proxyBeanMethods = false)
public class Config extends ReactiveConfig {
}
