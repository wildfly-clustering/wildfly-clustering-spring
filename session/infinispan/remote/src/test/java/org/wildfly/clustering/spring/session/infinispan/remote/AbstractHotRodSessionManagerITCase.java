/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.remote;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.session.spec.container.servlet.SessionServlet;
import org.wildfly.clustering.spring.session.context.SpringSessionFilter;

/**
 * @author Paul Ferraro
 */
public class AbstractHotRodSessionManagerITCase extends org.wildfly.clustering.spring.context.infinispan.remote.AbstractHotRodSessionManagerITCase {

	interface SessionManagementTesterConfiguration extends org.wildfly.clustering.session.container.SessionManagementTesterConfiguration {
		@Override
		default Class<?> getEndpointClass() {
			return SessionServlet.class;
		}
	}

	protected AbstractHotRodSessionManagerITCase() {
		super(new SessionManagementTesterConfiguration() {
		});
	}

	protected AbstractHotRodSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration);
	}

	protected AbstractHotRodSessionManagerITCase(Function<org.wildfly.clustering.session.container.SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration);
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration).addPackage(SpringSessionFilter.class.getPackage());
	}
}
