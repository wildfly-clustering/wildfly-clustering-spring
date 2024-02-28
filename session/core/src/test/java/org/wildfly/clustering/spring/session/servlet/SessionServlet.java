/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.wildfly.clustering.spring.web.SmokeITParameters;

/**
 * @author Paul Ferraro
 */
@WebServlet(SmokeITParameters.ENDPOINT_PATH)
public class SessionServlet extends HttpServlet {
	private static final long serialVersionUID = 2878267318695777395L;

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
			response.setHeader(SmokeITParameters.SESSION_ID, session.getId());
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession();
		response.setHeader(SmokeITParameters.SESSION_ID, session.getId());

		AtomicInteger value = (AtomicInteger) session.getAttribute(SmokeITParameters.VALUE);
		int result = 0;
		if (value == null) {
			value = new AtomicInteger(result);
			session.setAttribute(SmokeITParameters.VALUE, value);
		} else {
			result = value.incrementAndGet();
		}

		response.setIntHeader(SmokeITParameters.VALUE, result);
	}

	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			response.setHeader(SmokeITParameters.SESSION_ID, session.getId());
			session.invalidate();
		}
	}
}
