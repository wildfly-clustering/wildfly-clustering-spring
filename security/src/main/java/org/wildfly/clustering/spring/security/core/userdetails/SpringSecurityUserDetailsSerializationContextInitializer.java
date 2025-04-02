/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.core.userdetails;

import org.kohsuke.MetaInfServices;
import org.springframework.security.core.userdetails.UserDetails;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityUserDetailsSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityUserDetailsSerializationContextInitializer() {
		super(UserDetails.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new UserDetailsMarshaller());
	}
}
