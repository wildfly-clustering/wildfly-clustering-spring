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

package org.wildfly.clustering.web.spring.hotrod.servlet;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.wildfly.clustering.web.spring.servlet.SessionHandler;

/**
 * @author Paul Ferraro
 */
@WebServlet(SessionHandler.SERVLET_PATH)
public class SessionServlet extends HttpServlet implements SessionHandler {
    private static final long serialVersionUID = 2878267318695777395L;

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

        Integer value = (Integer) session.getAttribute(VALUE);
        if (value == null) {
            value = Integer.valueOf(0);
        } else {
            value = Integer.valueOf(value.intValue() + 1);
        }
        session.setAttribute(VALUE, value);

        response.setIntHeader(VALUE, value);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.changeSessionId();
        response.setHeader(SESSION_ID, id);
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