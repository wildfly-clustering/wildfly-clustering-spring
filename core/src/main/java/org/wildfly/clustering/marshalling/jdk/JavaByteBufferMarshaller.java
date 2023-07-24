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
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * A {@link ByteBufferMarshaller} based on Java serialization.
 * 
 * @author Paul Ferraro
 */
public class JavaByteBufferMarshaller implements ByteBufferMarshaller {
	private final Externalizer<ClassLoader> externalizer;

	public JavaByteBufferMarshaller(Externalizer<ClassLoader> externalizer) {
		this.externalizer = externalizer;
	}

	@Override
	public boolean isMarshallable(Object object) {
		return (object == null) || object instanceof Serializable;
	}

	@Override
	public Object readFrom(InputStream in) throws IOException {
		try (ObjectInput input = new ObjectInputStream(in, this.externalizer)) {
			return input.readObject();
		} catch (ClassNotFoundException e) {
			InvalidClassException exception = new InvalidClassException(e.getMessage());
			exception.initCause(e);
			throw exception;
		}
	}

	@Override
	public void writeTo(OutputStream out, Object object) throws IOException {
		try (ObjectOutput output = new ObjectOutputStream(out, this.externalizer)) {
			output.writeObject(object);
		}
	}
}
