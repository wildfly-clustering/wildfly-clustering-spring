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

package org.wildfly.clustering.web.spring;

import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Consumer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SpecificationProvider;

/**
 * @author Paul Ferraro
 */
public enum SpringSpecificationProvider implements SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener> {
    INSTANCE;

    @Override
    public HttpSession createHttpSession(ImmutableSession session, ServletContext context) {
        return new AbstractHttpSession() {
            @Override
            public String getId() {
                return session.getId();
            }

            @Override
            public ServletContext getServletContext() {
                return context;
            }

            @Override
            public boolean isNew() {
                return session.getMetaData().isNew();
            }

            @Override
            public long getCreationTime() {
                return session.getMetaData().getCreationTime().toEpochMilli();
            }

            @Override
            public long getLastAccessedTime() {
                return session.getMetaData().getLastAccessStartTime().toEpochMilli();
            }

            @Override
            public int getMaxInactiveInterval() {
                return (int) session.getMetaData().getMaxInactiveInterval().getSeconds();
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return Collections.enumeration(session.getAttributes().getAttributeNames());
            }

            @Override
            public Object getAttribute(String name) {
                return session.getAttributes().getAttribute(name);
            }

            @Override
            public void setAttribute(String name, Object value) {
                // Ignore
            }

            @Override
            public void removeAttribute(String name) {
                // Ignore
            }

            @Override
            public void invalidate() {
                // Ignore
            }

            @Override
            public void setMaxInactiveInterval(int interval) {
                // Ignore
            }
        };
    }

    @Override
    public Class<HttpSessionActivationListener> getHttpSessionActivationListenerClass() {
        return HttpSessionActivationListener.class;
    }

    @Override
    public Consumer<HttpSession> prePassivateNotifier(HttpSessionActivationListener listener) {
        return new Consumer<HttpSession>() {
            @Override
            public void accept(HttpSession session) {
                listener.sessionWillPassivate(new HttpSessionEvent(session));
            }
        };
    }

    @Override
    public Consumer<HttpSession> postActivateNotifier(HttpSessionActivationListener listener) {
        return new Consumer<HttpSession>() {
            @Override
            public void accept(HttpSession session) {
                listener.sessionDidActivate(new HttpSessionEvent(session));
            }
        };
    }

    @Override
    public HttpSessionActivationListener createListener(Consumer<HttpSession> prePassivate, Consumer<HttpSession> postActivate) {
        return new HttpSessionActivationListener() {
            @Override
            public void sessionWillPassivate(HttpSessionEvent event) {
                prePassivate.accept(event.getSession());
            }

            @Override
            public void sessionDidActivate(HttpSessionEvent event) {
                postActivate.accept(event.getSession());
            }
        };
    }
}
