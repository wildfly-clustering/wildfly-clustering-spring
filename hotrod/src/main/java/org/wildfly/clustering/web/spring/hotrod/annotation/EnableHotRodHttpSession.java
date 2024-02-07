/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.hotrod.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wildfly.clustering.spring.session.infinispan.remote.annotation.HotRodHttpSessionAnnotationConfiguration;
import org.wildfly.clustering.web.spring.annotation.SessionManager;

/**
 * Configures a session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.remote.annotation.EnableHotRodHttpSession} instead.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(HotRodHttpSessionAnnotationConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableHotRodHttpSession {
	HotRod config();
	SessionManager manager();
}
