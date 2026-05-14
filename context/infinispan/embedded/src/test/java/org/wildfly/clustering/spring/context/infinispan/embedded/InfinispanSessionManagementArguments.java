/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.Properties;

import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionManagementArguments;
import org.wildfly.clustering.spring.context.SessionPersistenceGranularity;

/**
 * @author Paul Ferraro
 */
public interface InfinispanSessionManagementArguments extends SessionManagementArguments, InfinispanConfiguration {

	@Override
	default Properties getProperties() {
		Properties properties = SessionManagementArguments.super.getProperties();
		properties.setProperty("infinispan.template", this.getTemplate());
		return properties;
	}

	static InfinispanSessionManagementArguments of() {
		return new InfinispanSessionManagementArguments() {
			@Override
			public SessionPersistenceGranularity getSessionPersistenceGranularity() {
				return SessionPersistenceGranularity.ATTRIBUTE;
			}

			@Override
			public SessionAttributeMarshaller getSessionMarshallerFactory() {
				return SessionAttributeMarshaller.PROTOSTREAM;
			}

			@Override
			public String getTemplate() {
				return "dist-tx";
			}
		};
	}
}
