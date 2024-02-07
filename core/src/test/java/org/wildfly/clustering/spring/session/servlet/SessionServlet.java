/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.ServletException;
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
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String query = request.getQueryString();
		this.getServletContext().log(String.format("[%s] %s%s", request.getMethod(), request.getRequestURI(), (query != null) ? '?' + query : ""));
		super.service(request, response);
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