/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.Objects;

import jakarta.servlet.http.HttpSession;

/**
 * Implements deprecated methods, as well as {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()}.
 * @author Paul Ferraro
 */
public abstract class AbstractHttpSession implements HttpSession {

	@Override
	public int hashCode() {
		return this.getId().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof HttpSession)) return false;
		HttpSession session = (HttpSession) object;
		return Objects.equals(this.getId(), session.getId()) && Objects.equals(this.getServletContext().getVirtualServerName(), session.getServletContext().getVirtualServerName()) && Objects.equals(this.getServletContext().getContextPath(), session.getServletContext().getContextPath());
	}

	@Override
	public String toString() {
		return this.getId();
	}
}
