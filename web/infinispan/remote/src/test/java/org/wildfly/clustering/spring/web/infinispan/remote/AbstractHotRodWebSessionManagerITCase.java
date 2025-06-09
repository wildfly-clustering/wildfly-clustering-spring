/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.remote;

import java.time.Duration;
import java.util.Optional;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;
import org.wildfly.clustering.spring.web.context.SessionHandler;
import org.wildfly.clustering.spring.web.servlet.DispatcherServlet;

/**
 * @author Paul Ferraro
 */
public class AbstractHotRodWebSessionManagerITCase extends org.wildfly.clustering.spring.context.infinispan.remote.AbstractHotRodSessionManagerITCase {

	protected AbstractHotRodWebSessionManagerITCase() {
		super(new SessionManagementTesterConfiguration() {
			@Override
			public Class<?> getEndpointClass() {
				return DispatcherServlet.class;
			}

			@Override
			public Optional<Duration> getFailoverGracePeriod() {
				return Optional.of(Duration.ofSeconds(2));
			}
		});
	}

	@Override
	public WebArchive createArchive(SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration).addPackage(SessionHandler.class.getPackage());
	}
}
