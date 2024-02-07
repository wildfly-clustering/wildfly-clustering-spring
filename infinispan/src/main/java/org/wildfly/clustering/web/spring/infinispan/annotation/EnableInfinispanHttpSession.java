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
import org.wildfly.clustering.spring.session.infinispan.embedded.annotation.InfinispanHttpSessionAnnotationConfiguration;
import org.wildfly.clustering.web.spring.annotation.SessionManager;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.embedded.annotation.EnableInfinispanHttpSession} instead.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(InfinispanHttpSessionAnnotationConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableInfinispanHttpSession {
	Infinispan config();
	SessionManager manager();
}
