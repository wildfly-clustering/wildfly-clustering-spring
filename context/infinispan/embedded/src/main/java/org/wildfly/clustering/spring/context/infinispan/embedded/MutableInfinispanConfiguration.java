/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.function.Consumer;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * @author Paul Ferraro
 */
public interface MutableInfinispanConfiguration extends InfinispanConfiguration, EmbeddedValueResolverAware, Consumer<AnnotationAttributes> {
	void setResource(String resource);

	void setTemplate(String templateName);
}
