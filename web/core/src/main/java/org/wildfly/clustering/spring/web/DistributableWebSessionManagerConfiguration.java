/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

import org.springframework.web.server.session.WebSessionIdResolver;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.SessionManager;

/**
 * @author Paul Ferraro
 * @param <B> batch type
 */
public interface DistributableWebSessionManagerConfiguration<B extends Batch> {
	SessionManager<Void, B> getSessionManager();
	WebSessionIdResolver getSessionIdentifierResolver();
}
