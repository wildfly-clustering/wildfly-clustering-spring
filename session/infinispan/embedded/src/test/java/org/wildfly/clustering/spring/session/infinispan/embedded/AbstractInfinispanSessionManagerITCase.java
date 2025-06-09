/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.session.spec.container.ServletSessionManagementTesterConfiguration;
import org.wildfly.clustering.spring.session.context.SpringSessionFilter;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionManagerITCase extends org.wildfly.clustering.spring.context.infinispan.embedded.AbstractInfinispanSessionManagerITCase {

	protected AbstractInfinispanSessionManagerITCase() {
		super(new ServletSessionManagementTesterConfiguration() {
		});
	}

	protected AbstractInfinispanSessionManagerITCase(ServletSessionManagementTesterConfiguration configuration) {
		super(configuration);
	}

	protected AbstractInfinispanSessionManagerITCase(Function<org.wildfly.clustering.session.container.SessionManagementTesterConfiguration, Tester> testerFactory, ServletSessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration);
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration).addPackage(SpringSessionFilter.class.getPackage());
	}
}
