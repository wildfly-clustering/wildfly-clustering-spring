/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web;

/**
 * Constants used in smoke tests.
 * @author Paul Ferraro
 */
public interface SmokeITParameters {
	String ENDPOINT_NAME = "session";
	String ENDPOINT_PATH = "/" + ENDPOINT_NAME;
	String VALUE = "value";
	String SESSION_ID = "session-id";
}
