/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.session.container.AbstractSessionManagerITCase;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionManagerITCase extends AbstractSessionManagerITCase<InfinispanSessionManagementArguments, WebArchive> {

	protected AbstractInfinispanSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration, WebArchive.class);
	}

	protected AbstractInfinispanSessionManagerITCase(Function<SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration, WebArchive.class);
	}

	@Override
	public WebArchive createArchive(InfinispanSessionManagementArguments arguments) {
		return super.createArchive(arguments).addAsWebInfResource("infinispan.xml");
	}
}
