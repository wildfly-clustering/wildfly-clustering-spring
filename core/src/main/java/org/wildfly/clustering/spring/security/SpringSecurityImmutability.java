/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.util.List;

import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.wildfly.clustering.server.immutable.Immutability;

/**
 * @author Paul Ferraro
 */
public enum SpringSecurityImmutability implements Immutability {
	INSTANCE;

	// SecurityContextImpl is technically mutable, but Spring treats it as immutable
	private final Immutability immutability = Immutability.classes(List.of(DefaultSavedRequest.class, SecurityContextImpl.class));

	@Override
	public boolean test(Object object) {
		return this.immutability.test(object);
	}
}
