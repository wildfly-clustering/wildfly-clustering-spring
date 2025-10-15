/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.infinispan.embedded.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanConfiguration;

/**
 * An annotation for configuring Infinispan.
 * @author Paul Ferraro
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Infinispan {
	/**
	 * Returns the path of the Infinispan configuration resource.
	 * @return the path of the Infinispan configuration resource.
	 */
	String resource() default InfinispanConfiguration.DEFAULT_CONFIGURATION_RESOURCE;

	/**
	 * Returns the name of a cache configuration.
	 * @return the name of a cache configuration.
	 */
	String template() default "";
}
