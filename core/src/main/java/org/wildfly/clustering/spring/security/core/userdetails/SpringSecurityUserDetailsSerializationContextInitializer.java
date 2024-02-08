/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.core.userdetails;

import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityUserDetailsSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityUserDetailsSerializationContextInitializer() {
		super("org.springframework.security.core.userdetails.proto");
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new UserDetailsMarshaller());
	}
}
