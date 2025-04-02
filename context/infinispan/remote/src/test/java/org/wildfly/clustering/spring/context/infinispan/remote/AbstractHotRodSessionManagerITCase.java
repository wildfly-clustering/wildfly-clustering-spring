/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.cache.ContainerProvider;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerContainer;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerExtension;
import org.wildfly.clustering.session.container.AbstractSessionManagerITCase;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodSessionManagerITCase extends AbstractSessionManagerITCase<WebArchive> implements UnaryOperator<Properties> {

	@RegisterExtension
	static final ContainerProvider<InfinispanServerContainer> INFINISPAN = new InfinispanServerExtension();

	protected AbstractHotRodSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration, WebArchive.class);
	}

	protected AbstractHotRodSessionManagerITCase(Function<SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration, WebArchive.class);
	}

	@Override
	public Properties apply(Properties properties) {
		InfinispanServerContainer container = INFINISPAN.getContainer();
		properties.setProperty("infinispan.server.host", container.getHost());
		properties.setProperty("infinispan.server.port", Integer.toString(container.getPort()));
		properties.setProperty("infinispan.server.username", container.getUsername());
		properties.setProperty("infinispan.server.password", String.valueOf(container.getPassword()));
		// TODO Figure out how to configure HASH_DISTRIBUTION_AWARE w/bridge networking
		properties.setProperty("infinispan.server.intelligence", (container.isPortMapping() ? ClientIntelligence.BASIC : ClientIntelligence.HASH_DISTRIBUTION_AWARE).name());
		properties.setProperty("infinispan.server.configuration", "<local-cache/>");
		return properties;
	}
}
