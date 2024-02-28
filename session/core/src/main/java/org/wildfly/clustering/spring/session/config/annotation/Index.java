/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a session attribute index.
 * @author Paul Ferraro
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Index {
	/**
	 * An arbitrary, but unique identifier of the index, intended as a shorter version of the index name.
	 * @return the unique identifier for the index
	 */
	String id();

	/**
	 * The name of the index
	 * @return the index name
	 */
	String name();
}
