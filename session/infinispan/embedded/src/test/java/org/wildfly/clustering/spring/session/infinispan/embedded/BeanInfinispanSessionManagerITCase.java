/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArguments;
import org.wildfly.clustering.spring.context.infinispan.embedded.InfinispanSessionManagementArgumentsProvider;
import org.wildfly.clustering.spring.session.context.xml.XmlContextLoaderListener;

/**
 * @author Paul Ferraro
 */
public class BeanInfinispanSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

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
