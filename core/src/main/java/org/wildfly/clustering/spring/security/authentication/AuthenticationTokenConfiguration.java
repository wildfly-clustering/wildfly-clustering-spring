/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication;

import java.util.LinkedList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

/**
 * @author Paul Ferraro
 */
public class AuthenticationTokenConfiguration {

	private Object principal = null;
	private Object credentials = "";
	private final List<GrantedAuthority> authorities = new LinkedList<>();
	private Object details = null;

	public Object getPrincipal() {
		return this.principal;
	}

	public AuthenticationTokenConfiguration setPrincipal(Object principal) {
		this.principal = principal;
		return this;
	}

	public Object getCredentials() {
		return this.credentials;
	}

	public AuthenticationTokenConfiguration setCredentials(Object credentials) {
		this.credentials = credentials;
		return this;
	}

	public List<GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	public AuthenticationTokenConfiguration addAuthority(GrantedAuthority authority) {
		this.authorities.add(authority);
		return this;
	}

	public Object getDetails() {
		return this.details;
	}

	public AuthenticationTokenConfiguration setDetails(Object details) {
		this.details = details;
		return this;
	}
}
