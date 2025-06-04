/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.spring.session.infinispan.remote;

import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.SessionManagementArgumentsProvider;
import org.wildfly.clustering.spring.context.SessionManagementParameters;
import org.wildfly.clustering.spring.session.context.xml.XmlContextLoaderListener;

/**
 * @author Paul Ferraro
 */
public class BeanHotRodSessionManagerITCase extends AbstractHotRodSessionManagerITCase {

	private final Properties properties = new Properties();

	@ParameterizedTest
	@ArgumentsSource(SessionManagementArgumentsProvider.class)
	public void test(SessionManagementParameters parameters) {
		this.properties.setProperty("session.granularity", parameters.getSessionPersistenceGranularity().name());
		this.properties.setProperty("session.marshaller", parameters.getSessionMarshallerFactory().name());
		this.run();
	}

	@Override
	public WebArchive createArchive(org.wildfly.clustering.session.container.SessionManagementTesterConfiguration configuration) {
		return super.createArchive(configuration)
				.addPackage(XmlContextLoaderListener.class.getPackage())
				.addAsWebInfResource("applicationContext.xml")
				.addAsWebInfResource(new PropertiesAsset(this.apply(this.properties)), "classes/application.properties")
				;
	}
}
