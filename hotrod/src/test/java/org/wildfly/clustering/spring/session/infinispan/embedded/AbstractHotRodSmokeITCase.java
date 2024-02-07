/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.net.URL;
import java.util.Properties;
import java.util.function.BiFunction;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.cache.ContainerProvider;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerContainer;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerExtension;
import org.wildfly.clustering.spring.session.AbstractSmokeITCase;
import org.wildfly.clustering.spring.session.PropertiesAsset;

/**
 * @author Paul Ferraro
 */
public class AbstractHotRodSmokeITCase extends AbstractSmokeITCase {

	@RegisterExtension
	static final ContainerProvider<InfinispanServerContainer> INFINISPAN = new InfinispanServerExtension();

	static WebArchive deployment(Class<? extends AbstractHotRodSmokeITCase> testClass) {
		InfinispanServerContainer container = INFINISPAN.getContainer();
		Properties properties = new Properties();
		properties.setProperty("infinispan.server.host", container.getHost());
		properties.setProperty("infinispan.server.port", Integer.toString(container.getPort()));
		properties.setProperty("infinispan.server.username", container.getUsername());
		properties.setProperty("infinispan.server.password", String.valueOf(container.getPassword()));
		// TODO Figure out how to configure HASH_DISTRIBUTION_AWARE w/bridge networking
		properties.setProperty("infinispan.server.intelligence", (container.isPortMapping() ? ClientIntelligence.BASIC : ClientIntelligence.HASH_DISTRIBUTION_AWARE).name());
		properties.setProperty("infinispan.server.template", DefaultTemplate.LOCAL.getTemplateName());
		return createWebArchive(testClass)
				.addAsWebInfResource(new PropertiesAsset(properties), "classes/application.properties")
				;
	}

	protected AbstractHotRodSmokeITCase() {
		super();
	}

	protected AbstractHotRodSmokeITCase(BiFunction<URL, URL, CloseableHttpClient> provider) {
		super(provider);
	}
}
