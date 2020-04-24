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

import java.util.function.Consumer;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationEventPublisher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionExpirationListener;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionExpirationListener implements SessionExpirationListener {

    private final Consumer<ImmutableSession> expireAction;
    private final Consumer<Runnable> classLoaderAction;

    public ImmutableSessionExpirationListener(ApplicationEventPublisher publisher, ServletContext context) {
        this.expireAction = new ImmutableSessionDestroyAction(publisher, context);
        this.classLoaderAction = new ContextClassLoaderAction(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void sessionExpired(ImmutableSession session) {
        this.classLoaderAction.accept(() -> this.expireAction.accept(session));
    }
}
