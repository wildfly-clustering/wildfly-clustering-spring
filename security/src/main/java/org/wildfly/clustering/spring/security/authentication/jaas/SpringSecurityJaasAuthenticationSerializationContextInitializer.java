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
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityJaasAuthenticationSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityJaasAuthenticationSerializationContextInitializer() {
		super(JaasAuthenticationToken.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new JaasGrantedAuthorityMarshaller());
		context.registerMarshaller(new CredentialAuthenticationMarshaller<>((principal, credentials) -> new JaasAuthenticationToken(principal, credentials, null), (entry, authorities) -> new JaasAuthenticationToken(entry.getKey(), entry.getValue(), authorities, null)).asMarshaller(JaasAuthenticationToken.class));
	}
}
