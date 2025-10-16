/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCacheContainer;

/**
 * A provider of a remote cache container.
 * @author Paul Ferraro
 */
public interface RemoteCacheContainerProvider {
	/**
	 * Returns the provided remote cache container.
	 * @return the provided remote cache container.
	 */
	RemoteCacheContainer getRemoteCacheContainer();
}
