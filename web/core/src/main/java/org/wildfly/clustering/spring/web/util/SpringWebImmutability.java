/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.springframework.web.util.HttpSessionMutexListener;
import org.wildfly.clustering.server.immutable.Immutability;

/**
 * The immutability tests for Spring Web objects.
 * @author Paul Ferraro
 */
public enum SpringWebImmutability implements Immutability {
	/** An immutability test for a mutex */
	MUTEX(Immutability.classes(Set.of(createMutex().getClass())));

	static Object createMutex() {
		try {
			// Mutex class is <sarcasm>conveniently</sarcasm> inaccessible
			Class<?> mutexClass = HttpSessionMutexListener.class.getClassLoader().loadClass(HttpSessionMutexListener.class.getName() + "$Mutex");
			Constructor<?> constructor = mutexClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	private final Immutability immutability;

	SpringWebImmutability(Immutability immutability) {
		this.immutability = immutability;
	}

	@Override
	public boolean test(Object object) {
		return this.immutability.test(object);
	}
}
