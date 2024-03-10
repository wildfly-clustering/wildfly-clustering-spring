/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

/**
 * @author Paul Ferraro
 */
public interface SessionManagementParameters {

	SessionPersistenceGranularity getSessionPersistenceGranularity();

	SessionAttributeMarshaller getSessionMarshallerFactory();
}
