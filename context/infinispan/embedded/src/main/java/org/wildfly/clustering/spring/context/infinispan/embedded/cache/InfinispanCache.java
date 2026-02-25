/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded.cache;

import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.spring.context.cache.AbstractCache;

/**
 * A {@link org.springframework.cache.Cache} based on an embedded Infinispan cache.
 * @author Paul Ferraro
 * @param <C> the marshalled value context
 */
public class InfinispanCache<C> extends AbstractCache<MarshalledValue<Object, C>, MarshalledValue<Object, C>> {
	/**
	 * Encapsulates the configuration of this cache
	 * @param <C> the marshalled value context
	 */
	public interface Configuration<C> extends AbstractCache.MarshalledValueConfiguration<C>, EmbeddedCacheConfiguration {
	}

	/**
	 * Constructs a new cache from the specified configuration.
	 * @param configuration the configuration of this cache
	 */
	public InfinispanCache(Configuration<C> configuration) {
		super(configuration);
	}
}
