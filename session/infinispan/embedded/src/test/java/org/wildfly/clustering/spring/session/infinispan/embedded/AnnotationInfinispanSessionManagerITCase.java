/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.session.spec.container.servlet.SessionServlet;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.infinispan.embedded.AbstractInfinispanInvalidationSessionManagerITCase;
import org.wildfly.clustering.spring.session.context.SpringSessionFilter;
import org.wildfly.clustering.spring.session.infinispan.embedded.context.Config;

/**
 * @author Paul Ferraro
 */
public class AnnotationInfinispanSessionManagerITCase extends AbstractInfinispanInvalidationSessionManagerITCase {

	public AnnotationInfinispanSessionManagerITCase() {
		super(new org.wildfly.clustering.session.container.SessionManagementTesterConfiguration() {
			@Override
			public Class<?> getEndpointClass() {
				return SessionServlet.class;
			}
		});
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration)
				.addAsWebInfResource(new PropertiesAsset(this.apply(new Properties())), "classes/application.properties")
				.addPackage(SpringSessionFilter.class.getPackage())
				.addPackage(Config.class.getPackage())
				;
	}

	@Test
	public void test() {
		this.run();
	}
}
