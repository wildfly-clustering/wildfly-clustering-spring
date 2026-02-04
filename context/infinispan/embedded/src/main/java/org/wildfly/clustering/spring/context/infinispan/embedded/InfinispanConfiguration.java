/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

/**
 * Encapsulates the Infinispan configuration
 * @author Paul Ferraro
 */
public interface InfinispanConfiguration {
	/** The default configuration resource path */
	String DEFAULT_CONFIGURATION_RESOURCE = "/WEB-INF/infinispan.xml";

	/**
	 * Returns the path of the configuration resource.
	 * @return the path of the configuration resource.
	 */
	default String getResource() {
		return DEFAULT_CONFIGURATION_RESOURCE;
	}

	/**
	 * Returns the name of a cache configuration.
	 * @return the name of a cache configuration.
	 */
	String getTemplate();
}
