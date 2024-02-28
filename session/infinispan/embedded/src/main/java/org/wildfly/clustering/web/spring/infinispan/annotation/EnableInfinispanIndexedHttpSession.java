/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.infinispan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanIndexedHttpSessionConfiguration;
import org.wildfly.clustering.web.spring.annotation.Indexing;
import org.wildfly.clustering.web.spring.annotation.SessionManager;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.embedded.config.annotation.EnableInfinispanIndexedHttpSession} instead.
 */
@Deprecated(forRemoval = true)
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
