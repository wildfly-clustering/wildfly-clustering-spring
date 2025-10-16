/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication.jaas;

import org.kohsuke.MetaInfServices;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.spring.security.authentication.CredentialAuthenticationMarshaller;

/**
 * The serialization context initializer for the {@link org.springframework.security.authentication.jaas} package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityJaasAuthenticationSerializationContextInitializer extends AbstractSerializationContextInitializer {
	/**
	 * Creates a new serialization context initializer.
	 */
	public SpringSecurityJaasAuthenticationSerializationContextInitializer() {
		super(JaasAuthenticationToken.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(JaasGrantedAuthorityMarshaller.INSTANCE);
		context.registerMarshaller(new CredentialAuthenticationMarshaller<>((principal, credentials) -> new JaasAuthenticationToken(principal, credentials, null), (entry, authorities) -> new JaasAuthenticationToken(entry.getKey(), entry.getValue(), authorities, null)).asMarshaller(JaasAuthenticationToken.class));
	}
}
