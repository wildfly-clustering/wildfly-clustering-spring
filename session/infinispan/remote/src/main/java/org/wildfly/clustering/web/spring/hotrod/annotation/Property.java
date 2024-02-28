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

/**
 * Configures a property.
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.context.infinispan.remote.config.annotation.Property} instead.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Property {
	/**
	 * The property name.
	 * @return the property name
	 */
	String name();

	/**
	 * The property value.
	 * @return the property value
	 */
	String value();
}
