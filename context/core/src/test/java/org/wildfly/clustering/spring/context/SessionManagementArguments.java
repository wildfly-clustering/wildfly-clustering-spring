/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.Properties;

/**
 * @author Paul Ferraro
 */
public interface SessionManagementArguments {

	SessionPersistenceGranularity getSessionPersistenceGranularity();

	SessionAttributeMarshaller getSessionMarshallerFactory();

	default Properties getProperties() {
		Properties properties = new Properties();
		properties.setProperty("session.granularity", this.getSessionPersistenceGranularity().name());
		properties.setProperty("session.marshaller", this.getSessionMarshallerFactory().name());
		return properties;
	}

	static SessionManagementArguments of() {
		return new SessionManagementArguments() {
			@Override
			public SessionPersistenceGranularity getSessionPersistenceGranularity() {
				return SessionPersistenceGranularity.ATTRIBUTE;
			}

			@Override
			public SessionAttributeMarshaller getSessionMarshallerFactory() {
				return SessionAttributeMarshaller.PROTOSTREAM;
			}
		};
	}
}
