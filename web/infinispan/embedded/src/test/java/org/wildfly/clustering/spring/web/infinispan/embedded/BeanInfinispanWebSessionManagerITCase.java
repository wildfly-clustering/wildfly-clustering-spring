/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.infinispan.embedded;

import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArgumentsProvider;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementParameters;
import org.wildfly.clustering.spring.web.context.xml.XmlContextLoaderListener;

/**
 * Integration test for reactive embedded Infinispan session manager configured via Spring bean XML.
 * Test executes against a combination of cache modes, transaction modes, granularities, and marshallers.
 * @author Paul Ferraro
 */
public class BeanInfinispanWebSessionManagerITCase extends AbstractInfinispanWebSessionManagerITCase {

	private final Properties properties = new Properties();

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_PLACEHOLDER)
	@ArgumentsSource(InfinispanSessionManagementArgumentsProvider.class)
	public void test(InfinispanSessionManagementParameters parameters) throws Exception {
		this.properties.setProperty("session.granularity", parameters.getSessionPersistenceGranularity().name());
		this.properties.setProperty("session.marshaller", parameters.getSessionMarshallerFactory().name());
		this.properties.setProperty("infinispan.template", parameters.getTemplate());
		this.run();
	}

	@Override
	public WebArchive createArchive(SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration)
				.addPackage(XmlContextLoaderListener.class.getPackage())
				.addAsWebInfResource("applicationContext.xml")
				.addAsWebInfResource(new PropertiesAsset(this.properties), "classes/application.properties")
				;
	}
}
