/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.util.Properties;
import java.util.function.Function;

import org.infinispan.client.hotrod.impl.HotRodURI;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.clustering.arquillian.Tester;
import org.wildfly.clustering.cache.ContainerProvider;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerContainer;
import org.wildfly.clustering.cache.infinispan.remote.InfinispanServerExtension;
import org.wildfly.clustering.session.container.AbstractSessionManagerITCase;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;
import org.wildfly.clustering.spring.context.PropertiesAsset;
import org.wildfly.clustering.spring.context.SessionManagementArguments;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodSessionManagerITCase extends AbstractSessionManagerITCase<SessionManagementArguments, WebArchive> {

	@RegisterExtension
	static final ContainerProvider<InfinispanServerContainer> INFINISPAN = new InfinispanServerExtension();

	protected AbstractHotRodSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration, WebArchive.class);
	}

	protected AbstractHotRodSessionManagerITCase(Function<SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration, WebArchive.class);
	}

	@Override
	public WebArchive createArchive(SessionManagementArguments arguments) {
		Properties properties = arguments.getProperties();
		HotRodURI uri = INFINISPAN.getContainer().get();
		properties.setProperty("infinispan.server.uri", uri.toString(true));
		// Use local cache since our remote cluster has only 1 member
		// Reduce expiration interval to speed up expiration verification
		properties.setProperty("infinispan.server.configuration", """
{ "local-cache" : { "encoding" : { "key" : { "media-type" : "application/octet-stream" }, "value" : { "media-type" : "application/octet-stream" }}, "expiration" : { "interval" : 1000 }, "locking" : { "isolation" : "REPEATABLE_READ" }, "transaction" : { "mode" : "NON_XA", "locking" : "PESSIMISTIC" }}}""");
		return super.createArchive(arguments).addAsWebInfResource(new PropertiesAsset(properties), "classes/application.properties");
	}
}
