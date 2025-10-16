/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wildfly.clustering.spring.context.config.annotation.SessionManager;
import org.wildfly.clustering.spring.context.infinispan.remote.config.annotation.HotRod;
import org.wildfly.clustering.spring.session.config.annotation.Indexing;
import org.wildfly.clustering.spring.session.infinispan.remote.config.HotRodIndexedHttpSessionConfiguration;

/**
 * Annotation defining the configuration of a Spring Session indexed repository
 * @author Paul Ferraro
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(HotRodIndexedHttpSessionConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableHotRodIndexedHttpSession {
	/**
	 * Returns the HotRod configuration.
	 * @return the HotRod configuration.
	 */
	HotRod config();

	/**
	 * Returns the session manager configuration.
	 * @return the session manager configuration.
	 */
	SessionManager manager();

	/**
	 * Returns the indexing configuration.
	 * @return the indexing configuration.
	 */
	Indexing indexing();
}
