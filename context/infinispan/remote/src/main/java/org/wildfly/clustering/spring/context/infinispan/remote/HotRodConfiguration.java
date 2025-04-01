/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.net.URI;
import java.util.Properties;

/**
 * @author Paul Ferraro
 */
public interface HotRodConfiguration {
	URI getUri();
	Properties getProperties();
	String getTemplateName();
	String getConfiguration();
}
