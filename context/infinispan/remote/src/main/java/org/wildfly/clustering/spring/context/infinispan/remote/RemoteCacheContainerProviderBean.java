/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.HotRodURI;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.executors.NonBlockingResource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.spring.context.AutoDestroyBean;

/**
 * A Spring bean that configures and provides a remote cache container.
 * @author Paul Ferraro
 */
public class RemoteCacheContainerProviderBean extends AutoDestroyBean implements RemoteCacheContainerProvider, InitializingBean, ResourceLoaderAware {

	private static final AtomicInteger COUNTER = new AtomicInteger(0);
	static class NonBlockingThreadGroup extends ThreadGroup implements NonBlockingResource {
		NonBlockingThreadGroup(String name) {
			super(name);
		}
	}

	private final HotRodConfiguration configuration;

	private RemoteCacheContainer container;
	private ClassLoader loader;

	/**
	 * Creates a remote cache container provider bean.
	 * @param configuration a HotRod configuration
	 */
	public RemoteCacheContainerProviderBean(HotRodConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public RemoteCacheContainer getRemoteCacheContainer() {
		return this.container;
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader.getClassLoader();
	}

	@Override
	public void afterPropertiesSet() {
		COUNTER.incrementAndGet();
		this.accept(() -> {
			// Stop RxJava schedulers when no longer in use
			if (COUNTER.decrementAndGet() == 0) {
				Schedulers.shutdown();
			}
		});

		ThreadPoolExecutor executor = new DefaultAsyncExecutorFactory().getExecutor(this.configuration.getProperties());
		URI uri = this.configuration.getUri();
		Configuration configuration = ((uri != null) ? HotRodURI.create(uri).toConfigurationBuilder() : new ConfigurationBuilder())
				.withProperties(this.configuration.getProperties())
				.asyncExecutorFactory().factory(new ExecutorFactory() {
					@Override
					public ExecutorService getExecutor(Properties p) {
						return executor;
					}
				})
				.marshaller(new UserMarshaller(MediaTypes.WILDFLY_PROTOSTREAM, new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(this.loader)).load(this.loader).build())))
				.build();
		this.accept(() -> {
			try {
				executor.awaitTermination(configuration.transactionTimeout(), TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		this.container = new RemoteCacheManager(configuration, false);
		this.container.start();
		this.accept(this.container::stop);
	}
}
