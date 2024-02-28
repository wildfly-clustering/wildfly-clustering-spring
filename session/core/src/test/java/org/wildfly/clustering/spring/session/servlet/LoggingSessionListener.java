/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * Detects support (or lack thereof) for HttpSessionListener notifications in Spring Session.
 * @author Paul Ferraro
 */
@WebListener
public class LoggingSessionListener implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		event.getSession().getServletContext().log("Session created: " + event.getSession().getId());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		event.getSession().getServletContext().log("Session destroyed: " + event.getSession().getId());
	}
}
