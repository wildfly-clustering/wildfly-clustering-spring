/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wildfly.clustering.spring.session.annotation.Indexing;
import org.wildfly.clustering.spring.session.annotation.SessionManager;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanIndexedHttpSessionConfiguration;

/**
 * @author Paul Ferraro
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(InfinispanIndexedHttpSessionConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableInfinispanIndexedHttpSession {
	Infinispan config();
	SessionManager manager();
	Indexing indexing();
}
