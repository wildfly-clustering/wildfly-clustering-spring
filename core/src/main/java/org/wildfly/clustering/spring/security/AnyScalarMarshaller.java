/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.spring.security;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;

/**
 * @author Paul Ferraro
 */
public class AnyScalarMarshaller<T> implements ScalarMarshaller<T> {

	private final Class<T> targetClass;

	public AnyScalarMarshaller(Class<T> targetClass) {
		this.targetClass = targetClass;
	}

	@Override
	public T readFrom(ProtoStreamReader reader) throws IOException {
		return this.targetClass.cast(Scalar.ANY.readFrom(reader));
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
		Scalar.ANY.writeTo(writer, value);
	}

	@Override
	public Class<? extends T> getJavaClass() {
		return this.targetClass;
	}

	@Override
	public WireType getWireType() {
		return Scalar.ANY.getWireType();
	}
}
