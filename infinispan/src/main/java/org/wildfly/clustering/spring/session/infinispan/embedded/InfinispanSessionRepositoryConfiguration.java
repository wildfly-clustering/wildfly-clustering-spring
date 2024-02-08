/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import org.wildfly.clustering.spring.session.SessionRepositoryConfiguration;

/**
 * @author Paul Ferraro
 */
public interface InfinispanSessionRepositoryConfiguration extends SessionRepositoryConfiguration {
	String getConfigurationResource();
	String getTemplateName();
}
