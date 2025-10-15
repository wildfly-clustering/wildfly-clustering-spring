/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.function.Consumer;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * A mutable Infinispan configuration.
 * @author Paul Ferraro
 */
public interface MutableInfinispanConfiguration extends InfinispanConfiguration, EmbeddedValueResolverAware, Consumer<AnnotationAttributes> {
	/**
	 * Specifies the path of the configuration resource.
	 * @param resource the path of the configuration resource.
	 */
	void setResource(String resource);

	/**
	 * Specifies the name of the target cache configuration.
	 * @param templateName the name of the target cache configuration.
	 */
	void setTemplate(String templateName);
}
