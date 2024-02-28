/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.core.context;

import org.kohsuke.MetaInfServices;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.spring.security.AnyScalarMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityContextSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityContextSerializationContextInitializer() {
		super("org.springframework.security.core.context.proto");
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new AnyScalarMarshaller<>(Authentication.class).toMarshaller(SecurityContextImpl.class, SecurityContextImpl::getAuthentication, SecurityContextImpl::new));
	}
}
