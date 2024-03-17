/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.util.function.Consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityContextMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource
	public void test(TesterFactory factory) {
		Consumer<SecurityContext> tester = factory.createTester();
		tester.accept(new SecurityContextImpl());
		tester.accept(new SecurityContextImpl(new UsernamePasswordAuthenticationToken("username", "password")));
	}
}
