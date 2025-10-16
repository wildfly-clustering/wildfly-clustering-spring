/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * A ProtoStream marshaller for an authentication token.
 * @author Paul Ferraro
 * @param <T> token type
 */
public class CredentialAuthenticationMarshaller<T extends AbstractAuthenticationToken> extends AuthenticationMarshaller<T> {
	/**
	 * Creates a credential authentication marshaller.
	 * @param unauthenticatedFactory the factory for creating an unauthenticated object
	 * @param authenticatedFactory the factory for creating an authenticated object
	 */
	public CredentialAuthenticationMarshaller(BiFunction<Object, Object, T> unauthenticatedFactory, BiFunction<Map.Entry<Object, Object>, List<GrantedAuthority>, T> authenticatedFactory) {
		super(new Function<>() {
			@Override
			public T apply(AuthenticationTokenConfiguration config) {
				Object principal = config.getPrincipal();
				Object credentials = config.getCredentials();
				List<GrantedAuthority> authorities = config.getAuthorities();
				T token = authorities.isEmpty() ? unauthenticatedFactory.apply(principal, credentials) : authenticatedFactory.apply(new SimpleImmutableEntry<>(principal, credentials), authorities);
				token.setDetails(config.getDetails());
				return token;
			}
		});
	}
}
