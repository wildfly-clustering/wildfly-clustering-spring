/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * @param <T> marshalling target type
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
