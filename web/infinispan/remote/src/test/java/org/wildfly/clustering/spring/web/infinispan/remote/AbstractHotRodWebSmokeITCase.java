/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.remote;

import java.util.Properties;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.cache.ContainerProvider;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerContainer;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerExtension;
import org.wildfly.clustering.spring.web.AbstractSmokeITCase;
import org.wildfly.clustering.spring.web.AbstractWebSmokeITCase;
import org.wildfly.clustering.spring.web.PropertiesAsset;

/**
 * @author Paul Ferraro
 */
public class AbstractHotRodWebSmokeITCase extends AbstractWebSmokeITCase {

	@RegisterExtension
	static final ContainerProvider<InfinispanServerContainer> INFINISPAN = new InfinispanServerExtension();

	protected static WebArchive deployment(Class<? extends AbstractSmokeITCase> testClass) {
		InfinispanServerContainer container = INFINISPAN.getContainer();
		Properties properties = new Properties();
		properties.setProperty("infinispan.server.host", container.getHost());
		properties.setProperty("infinispan.server.port", Integer.toString(container.getPort()));
		properties.setProperty("infinispan.server.username", container.getUsername());
		properties.setProperty("infinispan.server.password", String.valueOf(container.getPassword()));
		// TODO Figure out how to configure HASH_DISTRIBUTION_AWARE w/bridge networking
		properties.setProperty("infinispan.server.intelligence", (container.isPortMapping() ? ClientIntelligence.BASIC : ClientIntelligence.HASH_DISTRIBUTION_AWARE).name());
		properties.setProperty("infinispan.server.template", DefaultTemplate.LOCAL.getTemplateName());
		return AbstractWebSmokeITCase.deployment(testClass)
				.addAsWebInfResource(new PropertiesAsset(properties), "classes/application.properties")
				;
	}
}
