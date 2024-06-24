/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.impl.HotRodURI;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.executors.NonBlockingResource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.spring.context.AutoDestroyBean;

/**
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
	public void afterPropertiesSet() throws Exception {
		COUNTER.incrementAndGet();
		this.accept(() -> {
			// Stop RxJava schedulers when no longer in use
			if (COUNTER.decrementAndGet() == 0) {
				Schedulers.shutdown();
			}
		});

		URI uri = this.configuration.getUri();
		Configuration configuration = ((uri != null) ? HotRodURI.create(uri).toConfigurationBuilder() : new ConfigurationBuilder())
				.withProperties(this.configuration.getProperties())
				.marshaller(new UserMarshaller(MediaTypes.WILDFLY_PROTOSTREAM, new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(this.loader)).load(this.loader).build())))
				.asyncExecutorFactory().factory(new ExecutorFactory() {
					@Override
					public ThreadPoolExecutor getExecutor(Properties p) {
						ConfigurationProperties properties = new ConfigurationProperties(p);
						String threadNamePrefix = properties.getDefaultExecutorFactoryThreadNamePrefix();
						String threadNameSuffix = properties.getDefaultExecutorFactoryThreadNameSuffix();
						NonBlockingThreadGroup group = new NonBlockingThreadGroup(threadNamePrefix + "-group");
						ThreadFactory factory = new ThreadFactory() {
							private final AtomicInteger counter = new AtomicInteger(0);

							@Override
							public Thread newThread(Runnable task) {
								int threadIndex = this.counter.incrementAndGet();
								Thread thread = new Thread(group, task, threadNamePrefix + "-" + threadIndex + threadNameSuffix);
								thread.setDaemon(true);
								return thread;
							}
						};

						return new ThreadPoolExecutor(properties.getDefaultExecutorFactoryPoolSize(), properties.getDefaultExecutorFactoryPoolSize(), 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), new DefaultThreadFactory(factory, RemoteCacheContainerProviderBean.this.loader));
					}
				})
				.build();

		this.container = new RemoteCacheManager(configuration, false);
		this.container.start();
		this.accept(this.container::stop);
	}
}
