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
import java.io.OutputStream;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * An {@link java.io.ObjectInputStream} that annotates classes using a given {@link ClassLoaderResolver}.
 * @author Paul Ferraro
 */
public class ObjectOutputStream extends java.io.ObjectOutputStream {

    private final Externalizer<ClassLoader> externalizer;

    public ObjectOutputStream(OutputStream output, Externalizer<ClassLoader> externalizer) throws IOException {
        super(output);
        this.externalizer = externalizer;
    }

    @Override
    protected void annotateClass(Class<?> targetClass) throws IOException {
        this.externalizer.writeObject(this, targetClass.getClassLoader());
    }

    @Override
    protected void annotateProxyClass(Class<?> proxyClass) throws IOException {
        for (Class<?> interfaceClass : proxyClass.getInterfaces()) {
            this.externalizer.writeObject(this, interfaceClass.getClassLoader());
        }
        this.externalizer.writeObject(this, proxyClass.getClassLoader());
    }
}
