/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring;

import java.util.function.Function;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.SessionMarshallerFactory} instead.
 */
@Deprecated(forRemoval = true)
public enum SessionMarshallerFactory implements Function<ClassLoader, ByteBufferMarshaller> {

	JAVA(org.wildfly.clustering.spring.session.SessionMarshallerFactory.JAVA),
	JBOSS(org.wildfly.clustering.spring.session.SessionMarshallerFactory.JBOSS),
	PROTOSTREAM(org.wildfly.clustering.spring.session.SessionMarshallerFactory.PROTOSTREAM),
	;
	private final Function<ClassLoader, ByteBufferMarshaller> factory;

	SessionMarshallerFactory(Function<ClassLoader, ByteBufferMarshaller> factory) {
		this.factory = factory;
	}

	@Override
	public ByteBufferMarshaller apply(ClassLoader loader) {
		return this.factory.apply(loader);
	}
}
