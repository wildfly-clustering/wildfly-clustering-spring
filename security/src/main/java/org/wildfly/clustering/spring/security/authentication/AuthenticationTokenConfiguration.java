/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication;

import java.util.LinkedList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

/**
 * Encapsulates an authentication token configuration
 * @author Paul Ferraro
 */
class AuthenticationTokenConfiguration {

	private Object principal = null;
	private Object credentials = "";
	private final List<GrantedAuthority> authorities = new LinkedList<>();
	private Object details = null;

	Object getPrincipal() {
		return this.principal;
	}

	AuthenticationTokenConfiguration setPrincipal(Object principal) {
		this.principal = principal;
		return this;
	}

	Object getCredentials() {
		return this.credentials;
	}

	AuthenticationTokenConfiguration setCredentials(Object credentials) {
		this.credentials = credentials;
		return this;
	}

	List<GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	AuthenticationTokenConfiguration addAuthority(GrantedAuthority authority) {
		this.authorities.add(authority);
		return this;
	}

	Object getDetails() {
		return this.details;
	}

	AuthenticationTokenConfiguration setDetails(Object details) {
		this.details = details;
		return this;
	}
}
