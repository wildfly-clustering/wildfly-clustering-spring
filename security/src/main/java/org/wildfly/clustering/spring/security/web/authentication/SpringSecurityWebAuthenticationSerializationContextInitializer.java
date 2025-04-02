/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication;

import org.kohsuke.MetaInfServices;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityWebAuthenticationSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityWebAuthenticationSerializationContextInitializer() {
		super(WebAuthenticationDetails.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(HttpServletRequestMarshaller.INSTANCE.asMarshaller().wrap(WebAuthenticationDetails.class, MockHttpServletRequest::new, WebAuthenticationDetails::new));
	}
}
