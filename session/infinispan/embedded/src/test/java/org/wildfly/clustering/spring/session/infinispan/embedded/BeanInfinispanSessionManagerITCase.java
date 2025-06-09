/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArgumentsProvider;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementParameters;
import org.wildfly.clustering.spring.session.context.xml.XmlContextLoaderListener;

/**
 * @author Paul Ferraro
 */
public class BeanInfinispanSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

	private final Properties properties = new Properties();

	@ParameterizedTest
	@ArgumentsSource(InfinispanSessionManagementArgumentsProvider.class)
	public void test(InfinispanSessionManagementParameters parameters) {
		this.properties.setProperty("session.granularity", parameters.getSessionPersistenceGranularity().name());
		this.properties.setProperty("session.marshaller", parameters.getSessionMarshallerFactory().name());
		this.properties.setProperty("infinispan.template", parameters.getTemplate());
		this.run();
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration)
				.addPackage(XmlContextLoaderListener.class.getPackage())
				.addAsWebInfResource("applicationContext.xml")
				.addAsWebInfResource(new PropertiesAsset(this.properties), "classes/application.properties")
				;
	}
}
