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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.wildfly.clustering.web.spring.servlet.ServletService;
import org.wildfly.clustering.web.spring.servlet.ServletSession;

/**
 * @author Paul Ferraro
 */
public class SpringService implements ServletService {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    SpringService(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public ServletSession getSession(boolean create) {
        HttpSession session = this.request.getSession(create);
        return (session != null) ? new SpringSession(session) : null;
    }

    @Override
    public void setHeader(String name, int value) throws IOException {
        this.response.setIntHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) throws IOException {
        this.response.setHeader(name, value);
    }
}
