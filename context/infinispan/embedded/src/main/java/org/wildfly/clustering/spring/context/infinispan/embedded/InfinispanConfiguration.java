/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

/**
 * @author Paul Ferraro
 */
public interface InfinispanConfiguration {
	String DEFAULT_CONFIGURATION_RESOURCE = "/WEB-INF/infinispan.xml";

	String getConfigurationResource();
	String getTemplateName();
}
