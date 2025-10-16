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
	/** The schema for HTTP requests. */
	HTTP("http", 80),
	/** The schema for HTTPS requests. */
	HTTPS("https", 443),
	;
	private final String name;
	private final int defaultPort;

	Scheme(String name, int defaultPort) {
		this.name = name;
		this.defaultPort = defaultPort;
	}

	/**
	 * Returns the name of this scheme.
	 * @return the name of this scheme.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the default port associated with this scheme.
	 * @return the default port associated with this scheme.
	 */
	public int getDefaultPort() {
		return this.defaultPort;
	}

	/**
	 * Resolve the scheme with the specified name.
	 * @param name a schema name
	 * @return the scheme with the specified name.
	 * @throws IllegalArgumentException if there is no scheme with the specified name
	 */
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
