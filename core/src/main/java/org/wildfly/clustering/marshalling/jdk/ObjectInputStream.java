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

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * An {@link java.io.ObjectInputStream} that resolves classes using a given {@link ClassLoaderResolver}.
 * @author Paul Ferraro
 */
public class ObjectInputStream extends java.io.ObjectInputStream {

    private final Externalizer<ClassLoader> externalizer;

    public ObjectInputStream(InputStream input, Externalizer<ClassLoader> externalizer) throws IOException {
        super(input);
        this.externalizer = externalizer;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass description) throws IOException, ClassNotFoundException {
        String className = description.getName();
        return this.externalizer.readObject(this).loadClass(className);
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        Class<?>[] interfaceClasses = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            interfaceClasses[i] = this.externalizer.readObject(this).loadClass(interfaces[i]);
        }
        try {
            return Proxy.getProxyClass(this.externalizer.readObject(this), interfaceClasses);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
