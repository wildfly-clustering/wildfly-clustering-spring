/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.util;

import java.util.Set;

import org.wildfly.clustering.server.immutable.Immutability;

/**
 * The immutability tests for Spring Web objects.
 * @author Paul Ferraro
 */
public enum SpringWebImmutability implements Immutability {
	/** An immutability test for a mutex */
	MUTEX(Immutability.classes(Set.of(MutexFactory.INSTANCE.get().getClass())));

	private final Immutability immutability;

	SpringWebImmutability(Immutability immutability) {
		this.immutability = immutability;
	}

	@Override
	public boolean test(Object object) {
		return this.immutability.test(object);
	}
}
