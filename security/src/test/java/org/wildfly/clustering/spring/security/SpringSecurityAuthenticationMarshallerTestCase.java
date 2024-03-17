/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.function.UnaryOperator;

import javax.security.auth.login.LoginContext;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityAuthenticationMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource
	public void test(TesterFactory factory) {
		Tester<AbstractAuthenticationToken> tester = factory.createTester();
		UnaryOperator<AbstractAuthenticationToken> withDetails = token -> {
			token.setDetails("details");
			return token;
		};
		tester.accept(withDetails.apply(new AnonymousAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		tester.accept(withDetails.apply(new RememberMeAuthenticationToken("foo", "bar", Collections.emptySet())));
		tester.accept(withDetails.apply(new RememberMeAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		tester.accept(withDetails.apply(new UsernamePasswordAuthenticationToken("username", "password")));
		tester.accept(withDetails.apply(new UsernamePasswordAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		LoginContext context = mock(LoginContext.class);
		tester.accept(withDetails.apply(new JaasAuthenticationToken("username", "password", context)));
		tester.accept(withDetails.apply(new JaasAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")), context)));
	}
}
