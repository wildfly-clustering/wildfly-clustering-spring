/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring;

import java.util.Map;
import java.util.function.Function;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.SessionMarshallerFactory} instead.
 */
@Deprecated(forRemoval = true)
public enum SessionMarshallerFactory implements Function<Map.Entry<Environment, ResourceLoader>, ByteBufferMarshaller> {

	JAVA(org.wildfly.clustering.spring.context.SessionMarshallerFactory.JAVA),
	JBOSS(org.wildfly.clustering.spring.context.SessionMarshallerFactory.JBOSS),
	PROTOSTREAM(org.wildfly.clustering.spring.context.SessionMarshallerFactory.PROTOSTREAM),
	;
	private final Function<Map.Entry<Environment, ResourceLoader>, ByteBufferMarshaller> factory;

	SessionMarshallerFactory(Function<Map.Entry<Environment, ResourceLoader>, ByteBufferMarshaller> factory) {
		this.factory = factory;
	}

	@Override
	public ByteBufferMarshaller apply(Map.Entry<Environment, ResourceLoader> context) {
		return this.factory.apply(context);
	}
}
