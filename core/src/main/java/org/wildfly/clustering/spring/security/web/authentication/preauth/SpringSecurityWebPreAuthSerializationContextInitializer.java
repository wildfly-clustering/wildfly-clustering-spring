/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
