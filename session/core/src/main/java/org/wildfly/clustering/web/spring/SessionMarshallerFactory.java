/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring;

import java.util.function.BiFunction;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.SessionAttributeMarshaller} instead.
 */
@Deprecated(forRemoval = true)
public enum SessionMarshallerFactory implements BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> {

	JAVA(org.wildfly.clustering.spring.context.SessionAttributeMarshaller.JAVA),
	JBOSS(org.wildfly.clustering.spring.context.SessionAttributeMarshaller.JBOSS),
	PROTOSTREAM(org.wildfly.clustering.spring.context.SessionAttributeMarshaller.PROTOSTREAM),
	;
	private final BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> factory;

	SessionMarshallerFactory(BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> factory) {
		this.factory = factory;
	}

	@Override
	public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
		return this.factory.apply(environment, loader);
	}
}
