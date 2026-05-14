/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArguments;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArgumentsProvider;
import org.wildfly.clustering.spring.web.context.xml.XmlContextLoaderListener;

/**
 * Integration test for reactive embedded Infinispan session manager configured via Spring bean XML.
 * Test executes against a combination of cache modes, transaction modes, granularities, and marshallers.
 * @author Paul Ferraro
 */
public class BeanInfinispanWebSessionManagerITCase extends AbstractInfinispanWebSessionManagerITCase {

	@ParameterizedTest
	@ArgumentsSource(InfinispanSessionManagementArgumentsProvider.class)
	public void test(InfinispanSessionManagementArguments arguments) {
		this.accept(arguments);
	}

	@Override
	public WebArchive createArchive(InfinispanSessionManagementArguments arguments) {
		return super.createArchive(arguments)
				.addPackage(XmlContextLoaderListener.class.getPackage())
				.addAsWebInfResource("applicationContext.xml")
				.addAsWebInfResource(new PropertiesAsset(arguments.getProperties()), "classes/application.properties")
				;
	}
}
