/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.infinispan.embedded;

import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
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

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_PLACEHOLDER)
	@ArgumentsSource(InfinispanSessionManagementArgumentsProvider.class)
	public void test(InfinispanSessionManagementParameters parameters) throws Exception {
		Properties properties = new Properties();
		properties.setProperty("session.granularity", parameters.getSessionPersistenceGranularity().name());
		properties.setProperty("session.marshaller", parameters.getSessionMarshallerFactory().name());
		properties.setProperty("infinispan.template", parameters.getTemplate());
		WebArchive archive = this.get()
				.addPackage(XmlContextLoaderListener.class.getPackage())
				.addAsWebInfResource("applicationContext.xml")
				.addAsWebInfResource(new PropertiesAsset(properties), "classes/application.properties");
		this.accept(archive);
	}
}
