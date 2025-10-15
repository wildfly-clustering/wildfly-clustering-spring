/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A builder of an servlet request.
 * @author Paul Ferraro
 */
public class HttpServletRequestBuilder implements Supplier<HttpServletRequest> {

	private String remoteAddress = null;
	private String sessionId = null;

	/**
	 * Creates a builder of a servlet request.
	 */
	public HttpServletRequestBuilder() {
	}

	/**
	 * Specifies the remote address of this request.
	 * @param remoteAddress a remote address
	 * @return a reference to this builder
	 */
	public HttpServletRequestBuilder setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}

	/**
	 * Specifies the session identifier for this request.
	 * @param sessionId the identifier of a session
	 * @return a reference to this builder
	 */
	public HttpServletRequestBuilder setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public HttpServletRequest get() {
		return new MockHttpServletRequest(this.remoteAddress, this.sessionId);
	}
}
