/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication;

import org.kohsuke.MetaInfServices;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityAuthenticationSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityAuthenticationSerializationContextInitializer() {
		super("org.springframework.security.authentication.proto");
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new HashAuthenticationTokenMarshaller<>(AnonymousAuthenticationToken.class, AnonymousAuthenticationToken::getKeyHash));
		context.registerMarshaller(new HashAuthenticationTokenMarshaller<>(RememberMeAuthenticationToken.class, RememberMeAuthenticationToken::getKeyHash));
		context.registerMarshaller(new CredentialAuthenticationMarshaller<>(UsernamePasswordAuthenticationToken::new, (entry, authorities) -> new UsernamePasswordAuthenticationToken(entry.getKey(), entry.getValue(), authorities)).asMarshaller(UsernamePasswordAuthenticationToken.class));
	}
}
