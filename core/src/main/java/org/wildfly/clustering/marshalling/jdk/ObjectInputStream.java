/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * An {@link java.io.ObjectInputStream} that resolves classes using a given {@link ClassResolver}.
 * @author Paul Ferraro
 */
public class ObjectInputStream extends java.io.ObjectInputStream {

    private final ClassResolver resolver;

    public ObjectInputStream(InputStream input, ClassResolver resolver) throws IOException {
        super(input);
        this.resolver = resolver;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass description) throws IOException, ClassNotFoundException {
        return this.resolver.resolveClass(this, description.getName());
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        Class<?>[] interfaceClasses = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            interfaceClasses[i] = this.resolver.resolveClass(this, interfaces[i]);
        }
        try {
            return Proxy.getProxyClass(this.resolver.getClassLoader(), interfaceClasses);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
