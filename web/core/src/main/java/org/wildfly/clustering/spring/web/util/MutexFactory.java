/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import org.springframework.web.util.HttpSessionMutexListener;

/**
 * Factory for creating Spring's inconveniently inaccessible mutex.
 * @author Paul Ferraro
 */
public enum MutexFactory implements Supplier<Object> {
	/** Singleton instance */
	INSTANCE;

	private static final MethodHandle CONSTRUCTOR_HANDLE = new Supplier<MethodHandle>() {
		@Override
		public MethodHandle get() {
			try {
				Class<?> mutexClass = HttpSessionMutexListener.class.getClassLoader().loadClass(HttpSessionMutexListener.class.getName() + "$Mutex");
				return MethodHandles.privateLookupIn(mutexClass, MethodHandles.lookup()).findConstructor(mutexClass, MethodType.methodType(void.class));
			} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}.get();

	@Override
	public Object get() {
		try {
			return CONSTRUCTOR_HANDLE.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}
}
