/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.hotrod;

import org.wildfly.clustering.spring.session.infinispan.remote.HotRodSessionRepositoryConfiguration;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.remote.HotRodSessionRepository} instead.
 */
@Deprecated(forRemoval = true)
public class HotRodSessionRepository extends org.wildfly.clustering.spring.session.infinispan.remote.HotRodSessionRepository {

	public HotRodSessionRepository(HotRodSessionRepositoryConfiguration configuration) {
		super(configuration);
	}
}
