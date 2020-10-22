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

/**
 * @author Paul Ferraro
 */
public interface ServletHandler<REQUEST, RESPONSE> {
    final String SERVLET_NAME = "session";
    final String SERVLET_PATH = "/" + SERVLET_NAME;
    final String VALUE = "value";
    final String SESSION_ID = "session-id";

    static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    void doHead(REQUEST request, RESPONSE response) throws IOException;
    void doGet(REQUEST request, RESPONSE response) throws IOException;
    void doDelete(REQUEST request, RESPONSE response) throws IOException;

    default void doHead(ServletService service) throws IOException {
        ServletSession session = service.getSession(false);
        if (session != null) {
            service.setHeader(SESSION_ID, session.getId());
        }
    }

    default void doGet(ServletService service) throws IOException {
        ServletSession session = service.getSession();
        service.setHeader(SESSION_ID, session.getId());

        MutableInteger value = (MutableInteger) session.getAttribute(VALUE);
        if (value == null) {
            value = new MutableInteger(0);
            session.setAttribute(VALUE, value);
        } else {
            value.accept(value.getAsInt() + 1);
        }

        service.setHeader(VALUE, value.getAsInt());
    }

    default void doDelete(ServletService service) throws IOException {
        ServletSession session = service.getSession(false);
        if (session != null) {
            service.setHeader(SESSION_ID, session.getId());
            session.invalidate();
        }
    }
}