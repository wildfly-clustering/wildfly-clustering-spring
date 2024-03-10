/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.session.spec.container.servlet.SessionServlet;
import org.wildfly.clustering.spring.session.context.SpringSessionFilter;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionManagerITCase extends org.wildfly.clustering.spring.context.infinispan.embedded.AbstractInfinispanSessionManagerITCase {

	@Override
	public WebArchive get() {
		return super.get().addPackage(SpringSessionFilter.class.getPackage());
	}

	@Override
	public Class<?> getEndpointClass() {
		return SessionServlet.class;
	}
}
