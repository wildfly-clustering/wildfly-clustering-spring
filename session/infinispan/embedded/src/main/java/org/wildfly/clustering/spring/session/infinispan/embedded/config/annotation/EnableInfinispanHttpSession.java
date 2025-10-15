/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wildfly.clustering.spring.context.config.annotation.SessionManager;
import org.wildfly.clustering.spring.context.infinispan.embedded.config.annotation.Infinispan;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanHttpSessionConfiguration;

/**
 * Annotation defining the configuration of a Spring Session repository.
 * @author Paul Ferraro
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(InfinispanHttpSessionConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableInfinispanHttpSession {
	/**
	 * Returns the Infinispan configuration.
	 * @return the Infinispan configuration.
	 */
	Infinispan config();

	/**
	 * Returns the session manager configuration.
	 * @return the session manager configuration.
	 */
	SessionManager manager();
}
