/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.clustering.session.container.AbstractSessionManagerITCase;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionManagerITCase extends AbstractSessionManagerITCase {

	@Override
	public WebArchive get() {
		return super.get().addAsWebInfResource("infinispan.xml");
	}
}
