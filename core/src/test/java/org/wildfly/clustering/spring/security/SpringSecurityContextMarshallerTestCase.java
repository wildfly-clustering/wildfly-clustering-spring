/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityContextMarshallerTestCase {

	@Test
	public void test() throws IOException {
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new SecurityContextImpl());
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new SecurityContextImpl(new UsernamePasswordAuthenticationToken("username", "password")));
	}
}
