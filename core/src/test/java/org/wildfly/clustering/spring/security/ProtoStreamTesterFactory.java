/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.util.List;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.ByteBufferTestMarshaller;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.java.JavaTesterFactory;
import org.wildfly.clustering.spring.session.SessionMarshallerFactory;

/**
 * @author Paul Ferraro
 */
public enum ProtoStreamTesterFactory implements MarshallingTesterFactory {
	INSTANCE;

	private final ByteBufferMarshaller marshaller = SessionMarshallerFactory.PROTOSTREAM.apply(Thread.currentThread().getContextClassLoader());

	@Override
	public <T> MarshallingTester<T> createTester() {
		return new MarshallingTester<>(new ByteBufferTestMarshaller<>(this.marshaller), List.of(JavaTesterFactory.INSTANCE.getMarshaller()));
	}

	@Override
	public ByteBufferMarshaller getMarshaller() {
		return this.marshaller;
	}
}
