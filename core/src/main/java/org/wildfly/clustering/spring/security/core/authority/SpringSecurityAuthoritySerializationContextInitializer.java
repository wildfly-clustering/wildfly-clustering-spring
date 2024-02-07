/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.core.authority;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityAuthoritySerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityAuthoritySerializationContextInitializer() {
		super("org.springframework.security.core.authority.proto");
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(Scalar.STRING.cast(String.class).toMarshaller(SimpleGrantedAuthority.class, SimpleGrantedAuthority::getAuthority, SimpleGrantedAuthority::new));
	}
}
