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

	public DefaultBlockingThreadFactory(Class<?> targetClass) {
		super(targetClass, targetClass.getClassLoader());
	}
}
