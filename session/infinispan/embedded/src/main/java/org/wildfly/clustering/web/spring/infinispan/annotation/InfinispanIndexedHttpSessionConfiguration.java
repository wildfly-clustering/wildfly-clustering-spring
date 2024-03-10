/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.infinispan.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.wildfly.clustering.web.spring.SessionMarshallerFactory;
import org.wildfly.clustering.web.spring.SessionPersistenceGranularity;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanIndexedHttpSessionConfiguration} instead.
 */
@Deprecated(forRemoval = true)
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class InfinispanIndexedHttpSessionConfiguration extends org.wildfly.clustering.spring.session.infinispan.embedded.config.InfinispanIndexedHttpSessionConfiguration {

	@Autowired(required = false)
	public void setMarshallerFactory(SessionMarshallerFactory marshallerFactory) {
		this.setMarshaller(marshallerFactory);
	}

	@Autowired(required = false)
	public void setGranularity(SessionPersistenceGranularity granularity) {
		this.setPersistenceStrategy(granularity.get());
	}

	@Override
	public void setImportMetadata(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableInfinispanIndexedHttpSession.class.getName()));
		AnnotationAttributes manager = attributes.getAnnotation("manager");
		this.setMaxActiveSessions(manager.getNumber("maxActiveSessions").intValue());
		this.setMarshaller(manager.getEnum("marshallerFactory"));
		this.setGranularity(manager.getEnum("granularity"));
		this.accept(attributes);
	}
}
