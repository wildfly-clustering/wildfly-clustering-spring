/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication.preauth;

import org.kohsuke.MetaInfServices;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.spring.security.authentication.CredentialAuthenticationMarshaller;

/**
 * The serialization context initializer for the {@link org.springframework.security.web.authentication.preauth} package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityWebPreAuthSerializationContextInitializer extends AbstractSerializationContextInitializer {
	/**
	 * Creates a new serialization context initializer.
	 */
	public SpringSecurityWebPreAuthSerializationContextInitializer() {
		super(PreAuthenticatedAuthenticationToken.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new CredentialAuthenticationMarshaller<>(PreAuthenticatedAuthenticationToken::new, (entry, authorities) -> new PreAuthenticatedAuthenticationToken(entry.getKey(), entry.getValue(), authorities)).asMarshaller(PreAuthenticatedAuthenticationToken.class));
		context.registerMarshaller(PreAuthenticatedWebAuthenticationDetailsMarshaller.INSTANCE);
	}
}
