/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

/**
 * Enumeration of valid schemes for servlets.
 * @author Paul Ferraro
 */
public enum Scheme {
	HTTP("http", 80),
	HTTPS("https", 443),
	;
	private final String name;
	private final int defaultPort;

	Scheme(String name, int defaultPort) {
		this.name = name;
		this.defaultPort = defaultPort;
	}

	public String getName() {
		return this.name;
	}

	public int getDefaultPort() {
		return this.defaultPort;
	}

	public static Scheme resolve(String name) {
		switch (name) {
			case "http":
				return HTTP;
			case "https":
				return HTTPS;
			default:
				throw new IllegalArgumentException(name);
		}
	}
}
