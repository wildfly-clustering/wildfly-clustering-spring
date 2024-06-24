/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;
import org.wildfly.clustering.session.user.UserManager;

/**
 * @author Paul Ferraro
 */
public interface UserConfiguration {
	Map<String, UserManager<Void, Void, String, String>> getUserManagers();
	IndexResolver<Session> getIndexResolver();
}
