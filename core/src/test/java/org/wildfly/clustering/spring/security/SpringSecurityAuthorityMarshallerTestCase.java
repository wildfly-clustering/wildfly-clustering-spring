/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityAuthorityMarshallerTestCase {

	@Test
	public void test() throws IOException {
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new SimpleGrantedAuthority("foo"));
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new JaasGrantedAuthority("role", new UsernamePasswordAuthenticationToken("username", "password")));
	}
}
