/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded.cache;

import java.util.function.BiFunction;

import org.infinispan.manager.CacheContainer;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledKeyFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.clustering.spring.context.cache.AbstractCacheManager;

/**
 * A Spring {@link Cache} backed by an embedded Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanCacheManager extends AbstractCacheManager {

	private final CacheContainer container;

	/**
	 * Creates a new cache manager from the specified embedded Infinispan cache container.
	 * @param container a cache container
	 * @param marshallerFactory a cache entry marshaller
	 */
	public InfinispanCacheManager(CacheContainer container, BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory) {
		super(container, marshallerFactory);
		this.container = container;
	}

	@Override
	public @Nullable Cache getCache(String name) {
		CacheContainer container = this.container;
		ByteBufferMarshaller marshaller = this.getMarshaller();
		MarshalledValueFactory<ByteBufferMarshaller> keyFactory = new ByteBufferMarshalledKeyFactory(marshaller);
		MarshalledValueFactory<ByteBufferMarshaller> valueFactory = new ByteBufferMarshalledValueFactory(marshaller);
		return new InfinispanCache<>(new InfinispanCache.Configuration<ByteBufferMarshaller>() {
			@Override
			public MarshalledValueFactory<ByteBufferMarshaller> getKeyMarshalledValueFactory() {
				return keyFactory;
			}

			@Override
			public MarshalledValueFactory<ByteBufferMarshaller> getValueMarshalledValueFactory() {
				return valueFactory;
			}

			@Override
			public <K, V> org.infinispan.Cache<K, V> getCache() {
				return container.getCache(name);
			}
		});
	}
}
