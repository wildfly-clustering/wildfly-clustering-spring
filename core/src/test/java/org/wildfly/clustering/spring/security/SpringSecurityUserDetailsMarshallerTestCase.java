/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityUserDetailsMarshallerTestCase {

	@Test
	public void test() throws IOException {
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new User("username", "password", Collections.singleton(new SimpleGrantedAuthority("admin"))));
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new User("username", "password", false, false, false, false, Collections.singleton(new SimpleGrantedAuthority("admin"))));
	}
}
