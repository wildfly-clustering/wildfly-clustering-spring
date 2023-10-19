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

package org.wildfly.clustering.web.spring.security.authentication;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.wildfly.clustering.marshalling.protostream.FieldSetProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public class CredentialAuthenticationTokenMarshaller<T extends AbstractAuthenticationToken> extends FieldSetProtoStreamMarshaller<T, AuthenticationTokenConfiguration> {

	public CredentialAuthenticationTokenMarshaller(Class<T> tokenClass, BiFunction<Object, Object, T> unauthenticatedFactory, BiFunction<Map.Entry<Object, Object>, List<GrantedAuthority>, T> authenticatedFactory) {
		super(tokenClass, new AuthenticationMarshaller<>(config -> {
			Object principal = config.getPrincipal();
			Object credentials = config.getCredentials();
			List<GrantedAuthority> authorities = config.getAuthorities();
			T token = authorities.isEmpty() ? unauthenticatedFactory.apply(principal, credentials) : authenticatedFactory.apply(new SimpleImmutableEntry<>(principal, credentials), authorities);
			token.setDetails(config.getDetails());
			return token;
		}));
	}
}
