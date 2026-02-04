/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote.cache;

import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.spring.context.cache.AbstractCache;

/**
 * A {@link org.springframework.cache.Cache} based on a remote Infinispan cache.
 * @author Paul Ferraro
 * @param <C> the marshalled value context
 */
public class HotRodCache<C> extends AbstractCache<MarshalledValue<Object, C>, MarshalledValue<Object, C>> {
	/**
	 * Encapsulates the configuration of this cache
	 * @param <C> the marshalled value context
	 */
	public interface Configuration<C> extends MarshalledValueConfiguration<C>, RemoteCacheConfiguration {
	}

	/**
	 * Constructs a new cache from the specified configuration.
	 * @param configuration the configuration of this cache
	 */
	public HotRodCache(Configuration<C> configuration) {
		super(configuration);
	}
}
