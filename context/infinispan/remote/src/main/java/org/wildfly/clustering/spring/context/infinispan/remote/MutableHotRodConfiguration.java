/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.util.Properties;
import java.util.function.Consumer;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * A mutable HotRod configuration.
 * @author Paul Ferraro
 */
public interface MutableHotRodConfiguration extends HotRodConfiguration, EmbeddedValueResolverAware, Consumer<AnnotationAttributes> {

	/**
	 * Specifies the HotRod configuration URI.
	 * @param uri the HotRod configuration URI.
	 */
	void setUri(String uri);

	/**
	 * Specifies the configuration properties of the HotRod client.
	 * @param properties the configuration properties of the HotRod client.
	 */
	default void setProperties(Properties properties) {
		for (String name : properties.stringPropertyNames()) {
			this.setProperty(name, properties.getProperty(name));
		}
	}

	/**
	 * Species a configuration property of the HotRod client.
	 * @param name the configuration property name
	 * @param value the configuration property value
	 */
	void setProperty(String name, String value);

	/**
	 * Specifies the name of a cache configuration.
	 * @param templateName the name of a cache configuration
	 */
	void setTemplate(String templateName);

	/**
	 * Specifies a cache configuration
	 * @param configuration a cache configuration
	 */
	void setConfiguration(String configuration);
}
