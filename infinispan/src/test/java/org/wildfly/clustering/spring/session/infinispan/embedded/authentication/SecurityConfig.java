/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.wildfly.clustering.spring.security.authentication.AbstractSecurityConfig;
import org.wildfly.clustering.spring.session.SpringSession;
import org.wildfly.clustering.spring.session.infinispan.embedded.InfinispanSessionRepository;

/**
 * @author Paul Ferraro
 */
@EnableWebSecurity
public class SecurityConfig extends AbstractSecurityConfig {
	@Autowired
	InfinispanSessionRepository repository;

	@Override
	public FindByIndexNameSessionRepository<SpringSession> get() {
		return this.repository;
	}
}