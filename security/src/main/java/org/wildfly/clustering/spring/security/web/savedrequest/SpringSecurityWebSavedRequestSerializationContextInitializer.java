/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.savedrequest;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SpringSecurityWebSavedRequestSerializationContextInitializer extends AbstractSerializationContextInitializer {

	public SpringSecurityWebSavedRequestSerializationContextInitializer() {
		super("org.springframework.security.web.savedrequest.proto");
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new SavedRequestMarshaller());
		context.registerMarshaller(new SavedCookieMarshaller());
	}
}
