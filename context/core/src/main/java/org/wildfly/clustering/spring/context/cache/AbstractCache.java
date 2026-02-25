/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.cache;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.infinispan.commons.api.BasicCache;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.BasicCacheConfiguration;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.Marshaller;

/**
 * Generic Infinispan-based {@link Cache}.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public abstract class AbstractCache<K, V> implements Cache {
	/**
	 * Encapsulates the configuration of this cache.
	 * @param <C> the marshalled value context
	 */
	public interface MarshalledValueConfiguration<C> extends Configuration<MarshalledValue<Object, C>, MarshalledValue<Object, C>> {
		/**
		 * Returns the factory for creating marshalled values for cache keys.
		 * @return the factory for creating marshalled values for cache keys.
		 */
		MarshalledValueFactory<C> getKeyMarshalledValueFactory();
		/**
		 * Returns the factory for creating marshalled values for cache values.
		 * @return the factory for creating marshalled values for cache values.
		 */
		MarshalledValueFactory<C> getValueMarshalledValueFactory();

		@Override
		default Marshaller<Object, MarshalledValue<Object, C>> getKeyMarshaller() {
			return new MarshalledValueMarshaller<>(this.getKeyMarshalledValueFactory());
		}

		@Override
		default Marshaller<Object, MarshalledValue<Object, C>> getValueMarshaller() {
			return new MarshalledValueMarshaller<>(this.getValueMarshalledValueFactory());
		}
	}

	/**
	 * Encapsulates the configuration of this cache.
	 * @param <K> the cache key type
	 * @param <V> the cache value type
	 */
	public interface Configuration<K, V> extends BasicCacheConfiguration {
		/**
		 * Returns the marshaller for cache keys.
		 * @return the marshaller for cache keys.
		 */
		Marshaller<Object, K> getKeyMarshaller();
		/**
		 * Returns the marshaller for cache values.
		 * @return the marshaller for cache values.
		 */
		Marshaller<Object, V> getValueMarshaller();
	}

	private final Supplier<Batch> batchFactory;
	private final BasicCache<K, V> cache;
	private final BasicCache<K, V> readWriteCache;
	private final BasicCache<K, V> writeOnlyCache;
	private final Marshaller<Object, K> keyMarshaller;
	private final Marshaller<Object, V> valueMarshaller;

	/**
	 * Constructs a cache from the specified configuration
	 * @param configuration the configuration of this cache
	 */
	protected AbstractCache(Configuration<K, V> configuration) {
		this.batchFactory = configuration.getBatchFactory();
		this.cache = configuration.getCache();
		this.readWriteCache = configuration.getReadWriteCache();
		this.writeOnlyCache = configuration.getWriteOnlyCache();
		this.keyMarshaller = configuration.getKeyMarshaller();
		this.valueMarshaller = configuration.getValueMarshaller();
	}

	private Object read(V value) {
		try {
			return this.valueMarshaller.read(value);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private K writeKey(Object key) {
		try {
			return this.keyMarshaller.write(key);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private V writeValue(Object value) {
		try {
			return this.valueMarshaller.write(value);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private ValueWrapper wrap(V value) {
		return (value != null) ? () -> this.read(value) : null;
	}

	@Override
	public String getName() {
		return this.cache.getName();
	}

	@Override
	public Object getNativeCache() {
		return this.cache;
	}

	@Override
	public @Nullable ValueWrapper get(Object key) {
		try (Batch batch = this.batchFactory.get()) {
			V value = this.cache.get(key);
			return (value != null) ? () -> this.wrap(value) : null;
		}
	}

	@Override
	public <T> @Nullable T get(Object key, @Nullable Class<T> type) {
		try (Batch batch = this.batchFactory.get()) {
			V value = this.cache.get(this.writeKey(key));
			return (value != null) ? type.cast(this.read(value)) : null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T get(Object key, Callable<T> valueLoader) {
		Supplier<T> valueProvider = Supplier.call(valueLoader, Function.of(Consumer.throwing(exception -> new ValueRetrievalException(key, valueLoader, exception)), Supplier.empty()));
		try (Batch batch = this.batchFactory.get()) {
			V value = this.readWriteCache.computeIfAbsent(this.writeKey(key), Function.of(Consumer.empty(), valueProvider.thenApply(this::writeValue)));
			return (value != null) ? (T) this.read(value) : null;
		}
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		try (Batch batch = this.batchFactory.get()) {
			this.writeOnlyCache.put(this.writeKey(key), this.writeValue(value));
		}
	}

	@Override
	public @Nullable ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		try (Batch batch = this.batchFactory.get()) {
			V result = this.readWriteCache.putIfAbsent(this.writeKey(key), this.writeValue(value));
			return (result != null) ? () -> this.read(result) : null;
		}
	}

	@Override
	public void evict(Object key) {
		try (Batch batch = this.batchFactory.get()) {
			this.writeOnlyCache.remove(this.writeKey(key));
		}
	}

	@Override
	public boolean evictIfPresent(Object key) {
		try (Batch batch = this.batchFactory.get()) {
			return this.readWriteCache.remove(this.writeKey(key)) != null;
		}
	}

	@Override
	public void clear() {
		try (Batch batch = this.batchFactory.get()) {
			this.cache.clear();
		}
	}

	@Override
	public @Nullable CompletableFuture<?> retrieve(Object key) {
		try (Batch batch = this.batchFactory.get()) {
			// N.B. WTF is this method?!?
			// See upstream javadoc for details.
			return this.cache.getAsync(this.writeKey(key)).thenApply(this::wrap);
		}
	}
}
