/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.spring.web.infinispan.remote.context;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringValueResolver;
import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionPersistenceGranularity;
import org.wildfly.clustering.spring.context.config.annotation.SessionManager;
import org.wildfly.clustering.spring.context.infinispan.remote.config.annotation.HotRod;
import org.wildfly.clustering.spring.web.context.ReactiveConfig;
import org.wildfly.clustering.spring.web.infinispan.remote.config.annotation.EnableHotRodWebSession;

/**
 * Test configuration for session manager.
 * @author Paul Ferraro
 */
@PropertySource("classpath:application.properties")
@EnableHotRodWebSession(config = @HotRod(uri = "hotrod://${infinispan.server.username}:${infinispan.server.password}@${infinispan.server.host}:${infinispan.server.port}?tcp_no_delay=true", template = "${infinispan.server.template}"), manager = @SessionManager(marshaller = SessionAttributeMarshaller.PROTOSTREAM, granularity = SessionPersistenceGranularity.ATTRIBUTE))
@Configuration(proxyBeanMethods = false)
public class Config extends ReactiveConfig implements EmbeddedValueResolverAware {

	private StringValueResolver resolver;

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.resolver = resolver;
	}

	@Bean
	public StringValueResolver resolver() {
		return this.resolver;
	}
}
