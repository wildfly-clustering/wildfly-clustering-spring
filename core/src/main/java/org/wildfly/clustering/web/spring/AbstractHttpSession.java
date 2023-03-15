/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.spring;

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
