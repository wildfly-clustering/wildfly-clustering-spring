/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.spring.security.web.authentication.MockHttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityWebAuthenticationMarshallerTestCase {

	@Test
	public void test() throws IOException {
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new WebAuthenticationDetails(new MockHttpServletRequest("localhost", "ABCDEFG")));
		ProtoStreamTesterFactory.INSTANCE.createTester().test(new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(new MockHttpServletRequest("localhost", "ABCDEFG"), Collections.singletonList(new SimpleGrantedAuthority("admin"))));
	}
}
