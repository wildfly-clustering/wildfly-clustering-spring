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
 * @author Paul Ferraro
 */
public interface MutableHotRodConfiguration extends HotRodConfiguration, EmbeddedValueResolverAware, Consumer<AnnotationAttributes> {

	void setUri(String uri);

	default void setProperties(Properties properties) {
		for (String name : properties.stringPropertyNames()) {
			this.setProperty(name, properties.getProperty(name));
		}
	}

	void setProperty(String name, String value);

	void setTemplateName(String templateName);

	void setExpirationThreadPoolSize(int size);
}
