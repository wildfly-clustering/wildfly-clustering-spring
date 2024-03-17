/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.util.Collections;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.spring.security.web.authentication.MockHttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityWebAuthenticationMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource
	public void test(TesterFactory factory) {
		factory.createTester().accept(new WebAuthenticationDetails(new MockHttpServletRequest("localhost", "ABCDEFG")));
		factory.createTester().accept(new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(new MockHttpServletRequest("localhost", "ABCDEFG"), Collections.singletonList(new SimpleGrantedAuthority("admin"))));
	}
}
