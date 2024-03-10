/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.spring.context.infinispan.embedded.AbstractInfinispanSessionManagerITCase;
import org.wildfly.clustering.spring.web.context.SessionHandler;
import org.wildfly.clustering.spring.web.servlet.DispatcherServlet;

/**
 * @author Paul Ferraro
 */
public class AbstractInfinispanWebSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

	@Override
	public WebArchive get() {
		return super.get().addPackage(SessionHandler.class.getPackage());
	}

	@Override
	public Class<?> getEndpointClass() {
		return DispatcherServlet.class;
	}
}
