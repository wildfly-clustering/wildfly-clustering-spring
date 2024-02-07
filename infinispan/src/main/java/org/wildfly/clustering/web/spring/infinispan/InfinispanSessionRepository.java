/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.spring.infinispan;

import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepositoryConfiguration;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepository} instead.
 */
@Deprecated(forRemoval = true)
public class InfinispanSessionRepository extends org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepository {

	public InfinispanSessionRepository(InfinispanSessionRepositoryConfiguration configuration) {
		super(configuration);
	}
}
