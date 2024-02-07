/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.spring.session.infinispan.remote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;
import org.wildfly.clustering.spring.session.infinispan.remote.HotRodSessionRepository;

/**
 * Spring configuration bean for an indexed session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class HotRodIndexedHttpSessionConfiguration extends HotRodSessionRepositoryConfigurationBean {

	public HotRodIndexedHttpSessionConfiguration() {
		super(SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEXES, SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEX_RESOLVER);
	}

	@Bean
	public HotRodSessionRepository sessionRepository() {
		return new HotRodSessionRepository(this);
	}
}
