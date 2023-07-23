/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.spring.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(SessionServlet.SERVLET_PATH)
public class SessionServlet extends HttpServlet {
	private static final long serialVersionUID = 2878267318695777395L;

	private static final String SERVLET_NAME = "session";
	static final String SERVLET_PATH = "/" + SERVLET_NAME;
	public static final String VALUE = "value";
	public static final String SESSION_ID = "session-id";

	public static URI createURI(URL baseURL) throws URISyntaxException {
		return baseURL.toURI().resolve(SERVLET_NAME);
	}

	@Override
	public void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			response.setHeader(SESSION_ID, session.getId());
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession();
		response.setHeader(SESSION_ID, session.getId());

		MutableInteger value = (MutableInteger) session.getAttribute(VALUE);
		if (value == null) {
			value = new MutableInteger(0);
			session.setAttribute(VALUE, value);
		} else {
			value.accept(value.getAsInt() + 1);
		}

		response.setIntHeader(VALUE, value.getAsInt());
	}

	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			response.setHeader(SESSION_ID, session.getId());
			session.invalidate();
		}
	}
}