/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.util;

import org.springframework.web.util.HttpSessionMutexListener;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;

/**
 * @author Paul Ferraro
 */
public class UtilSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public UtilSerializationContextInitializer() {
		super(HttpSessionMutexListener.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(ProtoStreamMarshaller.of(SpringWebImmutability::createMutex));
	}
}
