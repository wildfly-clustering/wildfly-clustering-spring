/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;
import org.wildfly.clustering.spring.web.infinispan.embedded.config.annotation.EnableInfinispanWebSession;
import org.wildfly.clustering.spring.web.infinispan.embedded.context.ConfigContextLoaderListener;

/**
 * Integration test for reactive embedded Infinispan session manager configured via the {@link EnableInfinispanWebSession} annotation.
 * @author Paul Ferraro
 */
public class AnnotationInfinispanWebSessionManagerITCase extends AbstractInfinispanWebSessionManagerITCase {

	@Test
	public void test() {
		// N.B. this test is not parameterized, as it would otherwise require a config class and listener per test configuration.
		this.run();
	}

	@Override
	public WebArchive createArchive(SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration).addPackage(ConfigContextLoaderListener.class.getPackage());
	}
}
