/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.concurrent.ThreadFactory;

import org.infinispan.commons.executors.NonBlockingResource;
import org.wildfly.clustering.context.DefaultThreadFactory;

/**
 * Thread factory for non-blocking threads.
 * @author Paul Ferraro
 */
public class DefaultNonBlockingThreadFactory extends DefaultThreadFactory implements NonBlockingResource {

	public DefaultNonBlockingThreadFactory(Class<?> targetClass) {
		super(targetClass);
	}

	public DefaultNonBlockingThreadFactory(ThreadFactory factory) {
		super(factory);
	}
}