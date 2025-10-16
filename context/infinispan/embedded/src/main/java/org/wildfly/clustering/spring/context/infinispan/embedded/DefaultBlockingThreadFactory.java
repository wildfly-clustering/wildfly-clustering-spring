/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.infinispan.embedded;

import org.wildfly.clustering.context.DefaultThreadFactory;

/**
 * Thread factory for non-blocking threads.
 * @author Paul Ferraro
 */
public class DefaultBlockingThreadFactory extends DefaultThreadFactory {

	/**
	 * Creates a thread factory that creates blocking threads.
	 * @param targetClass the class providing the thread context class loader.
	 */
	public DefaultBlockingThreadFactory(Class<?> targetClass) {
		super(targetClass, targetClass.getClassLoader());
	}
}
