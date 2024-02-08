/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.time.Duration;

import jakarta.servlet.ServletContext;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.SessionManagerConfiguration;

/**
 * @author Paul Ferraro
 */
public interface JakartaSessionManagerConfiguration<B extends Batch> extends SessionManagerConfiguration<ServletContext> {

	@Override
	default Duration getTimeout() {
		return Duration.ofMinutes(this.getContext().getSessionTimeout());
	}
}
