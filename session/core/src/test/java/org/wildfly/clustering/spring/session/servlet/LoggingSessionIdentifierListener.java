/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;

/**
 * Detects support (or lack thereof) for HttpSessionIdListener notifications in Spring Session.
 * @author Paul Ferraro
 */
@WebListener
public class LoggingSessionIdentifierListener implements HttpSessionIdListener {

	@Override
	public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
		event.getSession().getServletContext().log("Session identifier changed, old = " + oldSessionId + ", new = " + event.getSession().getId());
	}
}
