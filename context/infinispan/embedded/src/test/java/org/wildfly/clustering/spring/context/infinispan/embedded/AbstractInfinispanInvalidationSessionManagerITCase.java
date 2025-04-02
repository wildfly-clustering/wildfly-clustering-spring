/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerContainer;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerExtension;
import org.wildfly.clustering.session.container.AbstractSessionManagerITCase;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;

/**
 * @author Paul Ferraro
 */
public class AbstractInfinispanInvalidationSessionManagerITCase extends AbstractSessionManagerITCase<WebArchive> implements UnaryOperator<Properties> {

	@RegisterExtension
	static final InfinispanServerExtension INFINISPAN = new InfinispanServerExtension();
	private static final String CACHE_NAME = "test";

	protected AbstractInfinispanInvalidationSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration, WebArchive.class);
	}

	protected AbstractInfinispanInvalidationSessionManagerITCase(Function<SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration, WebArchive.class);
	}

	@BeforeAll
	public static void init() {
		Configuration configuration = INFINISPAN.configure(new ConfigurationBuilder());
		try (RemoteCacheManager manager = new RemoteCacheManager(configuration)) {
			configuration.addRemoteCache(CACHE_NAME, builder -> builder.forceReturnValues(false).transactionMode(TransactionMode.NONE).configuration("<local-cache/>"));
			// Cache needs to be created on the server first
			RemoteCache<?, ?> cache = manager.getCache(CACHE_NAME);
			cache.start();
			cache.stop();
		}
	}

	@Override
	public WebArchive createArchive(SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration).addAsWebInfResource("infinispan-invalidation.xml", "infinispan.xml");
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
		properties.setProperty("infinispan.server.cache", CACHE_NAME);
		return properties;
	}
}
