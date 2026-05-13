/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;
import org.wildfly.clustering.spring.context.infinispan.embedded.AbstractInfinispanSessionManagerITCase;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArguments;
import org.wildfly.clustering.spring.web.context.SessionHandler;
import org.wildfly.clustering.spring.web.servlet.DispatcherServlet;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanWebSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

	protected AbstractInfinispanWebSessionManagerITCase() {
		super(new SessionManagementTesterConfiguration() {
			@Override
			public Class<?> getEndpointClass() {
				return DispatcherServlet.class;
			}
		});
	}

	@Override
	public WebArchive createArchive(InfinispanSessionManagementArguments arguments) {
		return super.createArchive(arguments).addPackage(SessionHandler.class.getPackage());
	}
}
