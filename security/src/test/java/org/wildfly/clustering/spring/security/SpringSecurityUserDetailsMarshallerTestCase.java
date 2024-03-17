/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityUserDetailsMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource
	public void test(TesterFactory factory) {
		Consumer<User> tester = factory.createTester();
		tester.accept(new User("username", "password", Collections.singleton(new SimpleGrantedAuthority("admin"))));
		tester.accept(new User("username", "password", false, false, false, false, Collections.singleton(new SimpleGrantedAuthority("admin"))));
	}
}
