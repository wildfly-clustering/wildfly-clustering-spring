/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.infinispan.embedded;

import org.infinispan.commons.executors.NonBlockingResource;
import org.wildfly.clustering.context.DefaultThreadFactory;

/**
 * Thread factory for non-blocking threads.
 * @author Paul Ferraro
 */
public class DefaultNonBlockingThreadFactory extends DefaultThreadFactory implements NonBlockingResource {

	/**
	 * Creates a thread factory that creates non-blocking threads.
	 * @param targetClass the class providing the thread context class loader.
	 */
	public DefaultNonBlockingThreadFactory(Class<?> targetClass) {
		super(targetClass, targetClass.getClassLoader());
	}
}
