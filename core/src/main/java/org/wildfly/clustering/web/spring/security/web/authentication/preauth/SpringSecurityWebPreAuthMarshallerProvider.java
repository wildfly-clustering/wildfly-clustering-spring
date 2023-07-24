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

package org.wildfly.clustering.web.spring.security.web.authentication.preauth;

import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.web.spring.security.authentication.CredentialAuthenticationTokenMarshaller;

/**
 * @author Paul Ferraro
 */
public enum SpringSecurityWebPreAuthMarshallerProvider implements ProtoStreamMarshallerProvider {

	TOKEN(new CredentialAuthenticationTokenMarshaller<>(PreAuthenticatedAuthenticationToken.class, PreAuthenticatedAuthenticationToken::new, (entry, authorities) -> new PreAuthenticatedAuthenticationToken(entry.getKey(), entry.getValue(), authorities))),
	DETAILS(new PreAuthenticatedWebAuthenticationDetailsMarshaller()),
	;
	private final ProtoStreamMarshaller<?> marshaller;

	SpringSecurityWebPreAuthMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
		this.marshaller = marshaller;
	}

	@Override
	public ProtoStreamMarshaller<?> getMarshaller() {
		return this.marshaller;
	}
}
