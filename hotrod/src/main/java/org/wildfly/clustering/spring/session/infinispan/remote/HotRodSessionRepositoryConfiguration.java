/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.remote;

import java.net.URI;
import java.util.Properties;

import org.wildfly.clustering.spring.session.SessionRepositoryConfiguration;

/**
 * Configuration for a session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 */
public interface HotRodSessionRepositoryConfiguration extends SessionRepositoryConfiguration {
	URI getUri();
	Properties getProperties();
	String getTemplateName();
	int getExpirationThreadPoolSize();
}
