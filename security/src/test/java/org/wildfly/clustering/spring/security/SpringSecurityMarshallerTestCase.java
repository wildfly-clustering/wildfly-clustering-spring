/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource
	public void test(TesterFactory factory) {
		factory.createTester(Scheme.class).run();
	}
}
