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

package org.wildfly.clustering.web.spring.security.web.authentication;

import javax.servlet.http.HttpServletRequest;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamBuilder;

/**
 * @author Paul Ferraro
 */
public class HttpServletRequestBuilder implements ProtoStreamBuilder<HttpServletRequest> {

    private String remoteAddress = null;
    private String sessionId = null;

    public HttpServletRequestBuilder setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public HttpServletRequestBuilder setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Override
    public HttpServletRequest build() {
        return new MockHttpServletRequest(this.remoteAddress, this.sessionId);
    }
}
