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
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityWebPreAuthSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityWebPreAuthSerializationContextInitializer() {
		super(PreAuthenticatedAuthenticationToken.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new CredentialAuthenticationMarshaller<>(PreAuthenticatedAuthenticationToken::new, (entry, authorities) -> new PreAuthenticatedAuthenticationToken(entry.getKey(), entry.getValue(), authorities)).asMarshaller(PreAuthenticatedAuthenticationToken.class));
		context.registerMarshaller(new PreAuthenticatedWebAuthenticationDetailsMarshaller());
	}
}
