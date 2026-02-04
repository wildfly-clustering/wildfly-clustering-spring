/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.cache;

import java.util.Collection;
import java.util.function.BiFunction;

import org.infinispan.commons.api.BasicCacheContainer;
import org.springframework.cache.CacheManager;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * An abstract {@link CacheManager} backed by Infinispan.
 * @author Paul Ferraro
 */
public abstract class AbstractCacheManager implements CacheManager, EnvironmentAware, ResourceLoaderAware {
	private final BasicCacheContainer container;
	private final BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory;

	private Environment environment;
	private ResourceLoader loader;

	/**
	 * Constructs a new cache manager.
	 * @param container the backing cache container
	 * @param marshallerFactory a factory for creating a cache entry marshaller
	 */
	protected AbstractCacheManager(BasicCacheContainer container, BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> marshallerFactory) {
		this.container = container;
		this.marshallerFactory = marshallerFactory;
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.container.getCacheNames();
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Returns a new cache entry marshaller.
	 * @return a new cache entry marshaller.
	 */
	protected ByteBufferMarshaller getMarshaller() {
		return this.marshallerFactory.apply(this.environment, this.loader);
	}
}
