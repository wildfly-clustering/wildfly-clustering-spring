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

package org.wildfly.clustering.web.spring.infinispan.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.wildfly.clustering.web.spring.annotation.IndexedHttpSessionConfiguration;
import org.wildfly.clustering.web.spring.infinispan.InfinispanSessionRepository;
import org.wildfly.clustering.web.spring.infinispan.InfinispanSessionRepositoryConfiguration;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class InfinispanIndexedHttpSessionConfiguration extends IndexedHttpSessionConfiguration implements InfinispanSessionRepositoryConfiguration {

	private String resource = "/WEB-INF/infinispan.xml";
	private String templateName = null;

	public InfinispanIndexedHttpSessionConfiguration() {
		super(EnableInfinispanIndexedHttpSession.class);
	}

	@Bean
	public InfinispanSessionRepository sessionRepository() {
		return new InfinispanSessionRepository(this);
	}

	@Override
	public String getConfigurationResource() {
		return this.resource;
	}

	@Override
	public String getTemplateName() {
		return this.templateName;
	}

	@Autowired(required = false)
	public void setConfigurationResource(String resource) {
		this.resource = resource;
	}

	@Autowired(required = false)
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	@Override
	public void accept(AnnotationAttributes attributes) {
		super.accept(attributes);
		AnnotationAttributes config = attributes.getAnnotation("config");
		this.resource = config.getString("resource");
		String templateName = config.getString("template");
		this.templateName = !templateName.isEmpty() ? templateName : null;
	}
}
