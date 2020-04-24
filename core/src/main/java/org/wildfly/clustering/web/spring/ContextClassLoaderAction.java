/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.spring;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Consumer;

/**
 * Performs some action with a given context class loader.
 * @author Paul Ferraro
 */
public class ContextClassLoaderAction implements Consumer<Runnable> {

    private static final PrivilegedAction<ClassLoader> GET_CONTEXT_CLASSLOADER_ACTION = () -> Thread.currentThread().getContextClassLoader();

    private interface ClassLoaderContext extends AutoCloseable {
        @Override
        void close();
    }

    private final WeakReference<ClassLoader> loader;

    public ContextClassLoaderAction() {
        this(AccessController.doPrivileged(GET_CONTEXT_CLASSLOADER_ACTION));
    }

    public ContextClassLoaderAction(ClassLoader loader) {
        this.loader = new WeakReference<>(loader);
    }

    @Override
    public void accept(Runnable task) {
        try (ClassLoaderContext context = getClassLoaderContext(this.loader.get())) {
            task.run();
        }
    }

    private static ClassLoaderContext getClassLoaderContext(ClassLoader loader) {
        if (loader == null) return () -> {};
        ClassLoader existingLoader = AccessController.doPrivileged(GET_CONTEXT_CLASSLOADER_ACTION);
        setContextClassLoader(loader);
        return () -> setContextClassLoader(existingLoader);
    }

    private static void setContextClassLoader(ClassLoader loader) {
        PrivilegedAction<Void> action = () -> {
            Thread.currentThread().setContextClassLoader(loader);
            return null;
        };
        AccessController.doPrivileged(action);
    }
}
