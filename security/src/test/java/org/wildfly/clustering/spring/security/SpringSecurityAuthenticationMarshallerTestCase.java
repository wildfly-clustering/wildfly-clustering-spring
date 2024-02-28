/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import java.util.function.UnaryOperator;

import javax.security.auth.login.LoginContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityAuthenticationMarshallerTestCase {

	@Test
	public void test() throws IOException {
		Tester<AbstractAuthenticationToken> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
		UnaryOperator<AbstractAuthenticationToken> withDetails = token -> {
			token.setDetails("details");
			return token;
		};
		tester.test(withDetails.apply(new AnonymousAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		tester.test(withDetails.apply(new RememberMeAuthenticationToken("foo", "bar", Collections.emptySet())));
		tester.test(withDetails.apply(new RememberMeAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		tester.test(withDetails.apply(new UsernamePasswordAuthenticationToken("username", "password")));
		tester.test(withDetails.apply(new UsernamePasswordAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		LoginContext context = mock(LoginContext.class);
		tester.test(withDetails.apply(new JaasAuthenticationToken("username", "password", context)));
		tester.test(withDetails.apply(new JaasAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")), context)));
	}
}
