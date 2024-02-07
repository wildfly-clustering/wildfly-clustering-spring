/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class HttpServletRequestBuilder implements Supplier<HttpServletRequest> {

	private String remoteAddress = null;
	private String sessionId = null;

	public HttpServletRequestBuilder setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}

	public HttpServletRequestBuilder setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public HttpServletRequest get() {
		return new MockHttpServletRequest(this.remoteAddress, this.sessionId);
	}
}
