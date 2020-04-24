/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSession<B extends Batch> implements org.springframework.session.Session {

    private final SessionManager<Void, B> manager;
    private final B batch;

    private volatile Session<Void> session;

    public DistributableSession(SessionManager<Void, B> manager, Session<Void> session, B batch) {
        this.manager = manager;
        this.session = session;
        this.batch = batch;
    }

    @Override
    public String changeSessionId() {
        Session<Void> oldSession = this.session;
        validate(oldSession);
        String id = this.manager.createIdentifier();
        try (BatchContext context = this.resumeBatch()) {
            Session<Void> newSession = this.manager.createSession(id);
            for (String name: oldSession.getAttributes().getAttributeNames()) {
                newSession.getAttributes().setAttribute(name, oldSession.getAttributes().getAttribute(name));
            }
            newSession.getMetaData().setMaxInactiveInterval(oldSession.getMetaData().getMaxInactiveInterval());
            newSession.getMetaData().setLastAccessedTime(oldSession.getMetaData().getLastAccessedTime());
            this.session = newSession;
            oldSession.invalidate();
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String name) {
        validate(this.session);
        return (T) this.session.getAttributes().getAttribute(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        validate(this.session);
        return this.session.getAttributes().getAttributeNames();
    }

    @Override
    public Instant getCreationTime() {
        validate(this.session);
        return this.session.getMetaData().getCreationTime();
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public Instant getLastAccessedTime() {
        validate(this.session);
        return this.session.getMetaData().getLastAccessedTime();
    }

    @Override
    public Duration getMaxInactiveInterval() {
        validate(this.session);
        return this.session.getMetaData().getMaxInactiveInterval();
    }

    @Override
    public boolean isExpired() {
        validate(this.session);
        return this.session.getMetaData().isExpired();
    }

    @Override
    public void removeAttribute(String name) {
        validate(this.session);
        this.session.getAttributes().removeAttribute(name);
        // N.B. org.springframework.session.web.http.HttpSessionAdapter already triggers HttpSessionBindingListener events
        // However, Spring Session violates the servlet specification by not triggering HttpSessionAttributeListener events
    }

    @Override
    public void setAttribute(String name, Object value) {
        validate(this.session);
        this.session.getAttributes().setAttribute(name, value);
        // N.B. org.springframework.session.web.http.HttpSessionAdapter already triggers HttpSessionBindingListener events
        // However, Spring Session violates the servlet specification by not triggering HttpSessionAttributeListener events
    }

    @Override
    public void setLastAccessedTime(Instant instant) {
        validate(this.session);
        this.session.getMetaData().setLastAccessedTime(instant);
    }

    @Override
    public void setMaxInactiveInterval(Duration duration) {
        validate(this.session);
        this.session.getMetaData().setMaxInactiveInterval(duration);
    }

    private static void validate(Session<Void> session) {
        if (!session.isValid()) {
            throw new IllegalStateException(String.format("Session %s is not valid", session.getId()));
        }
    }

    private BatchContext resumeBatch() {
        B batch = (this.batch.getState() != Batch.State.CLOSED) ? this.batch : null;
        return this.manager.getBatcher().resumeBatch(batch);
    }
}
