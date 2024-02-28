/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCacheContainer;

/**
 * @author Paul Ferraro
 */
public interface RemoteCacheContainerProvider {

	RemoteCacheContainer getRemoteCacheContainer();
}
