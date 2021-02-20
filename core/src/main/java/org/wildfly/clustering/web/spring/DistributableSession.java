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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * Spring Session implementation that delegates to a {@link Session} instance.
 * @author Paul Ferraro
 */
public class DistributableSession<B extends Batch> implements SpringSession {

    private final SessionManager<Void, B> manager;
    private final B batch;
    private final Instant startTime;
    private final IndexingConfiguration<B> indexing;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Session<Void> session;

    public DistributableSession(SessionManager<Void, B> manager, Session<Void> session, B batch, IndexingConfiguration<B> indexing) {
        this.manager = manager;
        this.session = session;
        this.batch = batch;
        this.indexing = indexing;
        this.startTime = session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
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
            newSession.getMetaData().setLastAccess(oldSession.getMetaData().getLastAccessStartTime(), oldSession.getMetaData().getLastAccessEndTime());
            this.session = newSession;
            oldSession.invalidate();
        }

        // Update indexes
        Map<String, String> indexes = this.indexing.getIndexResolver().resolveIndexesFor(this);
        for (Map.Entry<String, String> entry : indexes.entrySet()) {
            SSOManager<Void, String, String, Void, B> manager = this.indexing.getSSOManagers().get(entry.getKey());
            if (manager != null) {
                try (B batch = manager.getBatcher().createBatch()) {
                    SSO<Void, String, String, Void> sso = manager.findSSO(entry.getValue());
                    if (sso != null) {
                        sso.getSessions().removeSession(oldSession.getId());
                        sso.getSessions().addSession(id, id);
                    }
                }
            }
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
        return this.session.getMetaData().getLastAccessStartTime();
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
        this.setAttribute(name, null);
    }

    @Override
    public void setAttribute(String name, Object value) {
        validate(this.session);

        Map<String, String> oldIndexes = this.indexing.getIndexResolver().resolveIndexesFor(this);

        this.session.getAttributes().setAttribute(name, value);
        // N.B. org.springframework.session.web.http.HttpSessionAdapter already triggers HttpSessionBindingListener events
        // However, Spring Session violates the servlet specification by not triggering HttpSessionAttributeListener events

        // Update indexes
        Map<String, String> indexes = this.indexing.getIndexResolver().resolveIndexesFor(this);
        if (!oldIndexes.isEmpty() || !indexes.isEmpty()) {
            Set<String> indexNames = new TreeSet<>();
            indexNames.addAll(oldIndexes.keySet());
            indexNames.addAll(indexes.keySet());
            for (String indexName : indexNames) {
                String oldIndexValue = oldIndexes.get(indexName);
                String indexValue = indexes.get(indexName);
                if (!Objects.equals(indexValue, oldIndexValue)) {
                    SSOManager<Void, String, String, Void, B> manager = this.indexing.getSSOManagers().get(indexName);
                    try (B batch = manager.getBatcher().createBatch()) {
                        if (oldIndexValue != null) {
                            SSO<Void, String, String, Void> sso = manager.findSSO(oldIndexValue);
                            if (sso != null) {
                                sso.invalidate();
                            }
                        }
                        if (indexValue != null) {
                            String sessionId = this.getId();
                            SSO<Void, String, String, Void> sso = manager.createSSO(indexValue, null);
                            sso.getSessions().addSession(sessionId, sessionId);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setLastAccessedTime(Instant instant) {
        // We've already captured this
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

    @Override
    public void close() {
        // Workaround for WFLY-14466
        if (this.closed.compareAndSet(false, true)) {
            // According to §7.6 of the servlet specification:
            // The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
            this.session.getMetaData().setLastAccess(this.startTime, Instant.now());
            this.session.close();
        }
    }
}
