/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication.preauth;

import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.spring.security.authentication.CredentialAuthenticationMarshaller;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityWebPreAuthSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityWebPreAuthSerializationContextInitializer() {
		super("org.springframework.security.web.authentication.preauth.proto");
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new CredentialAuthenticationMarshaller<>(PreAuthenticatedAuthenticationToken::new, (entry, authorities) -> new PreAuthenticatedAuthenticationToken(entry.getKey(), entry.getValue(), authorities)).asMarshaller(PreAuthenticatedAuthenticationToken.class));
		context.registerMarshaller(new PreAuthenticatedWebAuthenticationDetailsMarshaller());
	}
}
