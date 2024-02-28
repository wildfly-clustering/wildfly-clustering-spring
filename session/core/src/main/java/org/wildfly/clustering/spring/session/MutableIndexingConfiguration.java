/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.session;

import java.util.Map;

import org.springframework.session.IndexResolver;
import org.springframework.session.Session;

/**
 * @author Paul Ferraro
 */
public interface MutableIndexingConfiguration extends IndexingConfiguration {
	void setIndexes(Map<String, String> indexes);

	void setIndexResolver(IndexResolver<Session> resolver);
}
