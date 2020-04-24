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

import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.web.cache.session.ImmutableSessionAttributesFilter;
import org.wildfly.clustering.web.cache.session.SessionAttributesFilter;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionDestroyAction implements Consumer<ImmutableSession> {

    private final ApplicationEventPublisher publisher;
    private final ServletContext context;

    public ImmutableSessionDestroyAction(ApplicationEventPublisher publisher, ServletContext context) {
        this.publisher = publisher;
        this.context = context;
    }

    @Override
    public void accept(ImmutableSession session) {
        this.publisher.publishEvent(new SessionDestroyedEvent(this, new DistributableImmutableSession(session)));
        SessionAttributesFilter filter = new ImmutableSessionAttributesFilter(session);
        HttpSession httpSession = SpringSpecificationProvider.INSTANCE.createHttpSession(session, this.context);
        for (Map.Entry<String, HttpSessionBindingListener> entry : filter.getAttributes(HttpSessionBindingListener.class).entrySet()) {
            HttpSessionBindingListener listener = entry.getValue();
            try {
                listener.valueUnbound(new HttpSessionBindingEvent(httpSession, entry.getKey(), listener));
            } catch (Throwable e) {
                this.context.log(e.getMessage(), e);
            }
        }
    }
}
