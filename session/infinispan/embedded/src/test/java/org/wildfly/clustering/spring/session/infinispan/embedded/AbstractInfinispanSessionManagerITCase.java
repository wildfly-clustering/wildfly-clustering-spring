/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.session.spec.container.servlet.SessionServlet;
import org.wildfly.clustering.spring.session.context.SpringSessionFilter;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionManagerITCase extends org.wildfly.clustering.spring.context.infinispan.embedded.AbstractInfinispanSessionManagerITCase {

	interface SessionManagementTesterConfiguration extends org.wildfly.clustering.session.container.SessionManagementTesterConfiguration {
		@Override
		default Class<?> getEndpointClass() {
			return SessionServlet.class;
		}
	}

	protected AbstractInfinispanSessionManagerITCase() {
		super(new SessionManagementTesterConfiguration() {
		});
	}

	protected AbstractInfinispanSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration);
	}

	protected AbstractInfinispanSessionManagerITCase(Function<org.wildfly.clustering.session.container.SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration);
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration).addPackage(SpringSessionFilter.class.getPackage());
	}
}
