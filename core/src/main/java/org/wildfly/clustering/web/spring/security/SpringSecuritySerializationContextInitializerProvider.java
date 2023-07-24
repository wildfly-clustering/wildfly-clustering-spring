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

package org.wildfly.clustering.web.spring.security;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.web.spring.security.authentication.SpringSecurityAuthenticationMarshallerProvider;
import org.wildfly.clustering.web.spring.security.authentication.jaas.SpringSecurityJaasAuthenticationMarshallerProvider;
import org.wildfly.clustering.web.spring.security.core.authority.SpringSecurityAuthorityMarshallerProvider;
import org.wildfly.clustering.web.spring.security.core.context.SpringSecurityContextMarshallerProvider;
import org.wildfly.clustering.web.spring.security.core.userdetails.SpringSecurityUserDetailsMarshallerProvider;
import org.wildfly.clustering.web.spring.security.web.authentication.SpringSecurityWebAuthenticationMarshallerProvider;
import org.wildfly.clustering.web.spring.security.web.authentication.preauth.SpringSecurityWebPreAuthMarshallerProvider;
import org.wildfly.clustering.web.spring.security.web.savedrequest.SpringSecurityWebSavedRequestMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum SpringSecuritySerializationContextInitializerProvider implements SerializationContextInitializerProvider {

	SPRING_SECURITY(new SpringSecuritySerializationContextInitializer()),
	AUTHENTICATION(new ProviderSerializationContextInitializer<>("org.springframework.security.authentication.proto", SpringSecurityAuthenticationMarshallerProvider.class)),
	JAAS(new ProviderSerializationContextInitializer<>("org.springframework.security.authentication.jaas.proto", SpringSecurityJaasAuthenticationMarshallerProvider.class)),
	AUTHORITY(new ProviderSerializationContextInitializer<>("org.springframework.security.core.authority.proto", SpringSecurityAuthorityMarshallerProvider.class)),
	CONTEXT(new ProviderSerializationContextInitializer<>("org.springframework.security.core.context.proto", SpringSecurityContextMarshallerProvider.class)),
	USER_DETAILS(new ProviderSerializationContextInitializer<>("org.springframework.security.core.userdetails.proto", SpringSecurityUserDetailsMarshallerProvider.class)),
	WEB_AUTHENTICATION(new ProviderSerializationContextInitializer<>("org.springframework.security.web.authentication.proto", SpringSecurityWebAuthenticationMarshallerProvider.class)),
	PRE_AUTH_WEB_AUTHENTICATION(new ProviderSerializationContextInitializer<>("org.springframework.security.web.authentication.preauth.proto", SpringSecurityWebPreAuthMarshallerProvider.class)),
	SAVED_REQUEST(new ProviderSerializationContextInitializer<>("org.springframework.security.web.savedrequest.proto", SpringSecurityWebSavedRequestMarshallerProvider.class)),
	;
	private final SerializationContextInitializer initializer;

	SpringSecuritySerializationContextInitializerProvider(SerializationContextInitializer initializer) {
		this.initializer = initializer;
	}

	@Override
	public SerializationContextInitializer getInitializer() {
		return this.initializer;
	}
}
