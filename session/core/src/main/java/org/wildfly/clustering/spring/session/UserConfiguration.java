/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.user.UserManager;

/**
 * @author Paul Ferraro
 */
public interface UserConfiguration<B extends Batch> {
	Map<String, UserManager<Void, Void, String, String, B>> getUserManagers();
	IndexResolver<Session> getIndexResolver();
}
