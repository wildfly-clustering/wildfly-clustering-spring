/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.util.function.Consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityAuthorityMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource
	public void test(TesterFactory factory) {
		Consumer<GrantedAuthority> tester = factory.createTester();
		tester.accept(new SimpleGrantedAuthority("foo"));
		tester.accept(new JaasGrantedAuthority("role", new UsernamePasswordAuthenticationToken("username", "password")));
	}
}
