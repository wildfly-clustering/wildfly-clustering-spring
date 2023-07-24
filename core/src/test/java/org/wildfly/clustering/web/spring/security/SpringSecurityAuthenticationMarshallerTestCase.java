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

package org.wildfly.clustering.web.spring.security;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import java.util.function.UnaryOperator;

import javax.security.auth.login.LoginContext;

import org.junit.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityAuthenticationMarshallerTestCase {

	@Test
	public void test() throws IOException {
		UnaryOperator<AbstractAuthenticationToken> withDetails = token -> {
			token.setDetails("details");
			return token;
		};
		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new AnonymousAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new RememberMeAuthenticationToken("foo", "bar", Collections.emptySet())));
		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new RememberMeAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new UsernamePasswordAuthenticationToken("username", "password")));
		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new UsernamePasswordAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")))));

		LoginContext context = mock(LoginContext.class);
		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new JaasAuthenticationToken("username", "password", context)));
		ProtoStreamTesterFactory.INSTANCE.createTester().test(withDetails.apply(new JaasAuthenticationToken("foo", "bar", Collections.singletonList(new SimpleGrantedAuthority("admin")), context)));
	}
}
