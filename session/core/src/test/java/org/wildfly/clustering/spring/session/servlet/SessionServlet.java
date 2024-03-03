/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.wildfly.clustering.session.container.SessionManagementEndpointConfiguration;

/**
 * @author Paul Ferraro
 */
@WebServlet(SessionManagementEndpointConfiguration.ENDPOINT_PATH)
public class SessionServlet extends HttpServlet {
	private static final long serialVersionUID = 2878267318695777395L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String query = request.getQueryString();
		this.getServletContext().log(String.format("[%s] %s%s", request.getMethod(), request.getRequestURI(), (query != null) ? '?' + query : ""));
		super.service(request, response);

		HttpSession session = request.getSession(false);
		if (session != null) {
			response.setHeader(SessionManagementEndpointConfiguration.SESSION_ID, session.getId());
		}
	}

	@Override
	public void doHead(HttpServletRequest request, HttpServletResponse response) {
		recordSession(request, response, AtomicInteger::get);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		recordSession(request, response, AtomicInteger::incrementAndGet);
	}

	private static void recordSession(HttpServletRequest request, HttpServletResponse response, java.util.function.ToIntFunction<AtomicInteger> count) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			AtomicInteger counter = (AtomicInteger) session.getAttribute(SessionManagementEndpointConfiguration.COUNTER);
			if (counter != null) {
				response.setIntHeader(SessionManagementEndpointConfiguration.COUNTER, count.applyAsInt(counter));
			}
			UUID value = (UUID) session.getAttribute(SessionManagementEndpointConfiguration.IMMUTABLE);
			if (value != null) {
				response.setHeader(SessionManagementEndpointConfiguration.IMMUTABLE, value.toString());
			}
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(true);

		UUID immutableValue = UUID.randomUUID();
		session.setAttribute(SessionManagementEndpointConfiguration.IMMUTABLE, immutableValue);
		response.addHeader(SessionManagementEndpointConfiguration.IMMUTABLE, immutableValue.toString());

		AtomicInteger counter = new AtomicInteger(0);
		session.setAttribute(SessionManagementEndpointConfiguration.COUNTER, counter);
		response.addIntHeader(SessionManagementEndpointConfiguration.COUNTER, counter.get());
	}

	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
