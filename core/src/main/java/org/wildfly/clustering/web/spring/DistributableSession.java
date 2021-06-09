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
import org.wildfly.clustering.web.session.oob.OOBSession;
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
        String id = this.manager.createIdentifier();
        try (BatchContext context = this.resumeBatch()) {
            Session<Void> newSession = this.manager.createSession(id);
            try {
                for (String name: oldSession.getAttributes().getAttributeNames()) {
                    newSession.getAttributes().setAttribute(name, oldSession.getAttributes().getAttribute(name));
                }
                newSession.getMetaData().setMaxInactiveInterval(oldSession.getMetaData().getMaxInactiveInterval());
                newSession.getMetaData().setLastAccess(oldSession.getMetaData().getLastAccessStartTime(), oldSession.getMetaData().getLastAccessEndTime());
                oldSession.invalidate();
                this.session = newSession;
            } catch (IllegalStateException e) {
                if (!oldSession.isValid()) {
                    oldSession.close();
                }
                newSession.invalidate();
                throw e;
            }
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
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return (T) session.getAttributes().getAttribute(name);
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return session.getAttributes().getAttributeNames();
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public Instant getCreationTime() {
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().getCreationTime();
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public Instant getLastAccessedTime() {
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().getLastAccessStartTime();
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public Duration getMaxInactiveInterval() {
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().getMaxInactiveInterval();
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public boolean isExpired() {
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().isExpired();
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public void removeAttribute(String name) {
        this.setAttribute(name, null);
    }

    @Override
    public void setAttribute(String name, Object value) {
        Map<String, String> oldIndexes = this.indexing.getIndexResolver().resolveIndexesFor(this);

        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            session.getAttributes().setAttribute(name, value);
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }

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
                            String sessionId = session.getId();
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
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            session.getMetaData().setMaxInactiveInterval(duration);
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public boolean isNew() {
        Session<Void> session = this.session;
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().isNew();
        } catch (IllegalStateException e) {
            if (!session.isValid()) {
                session.close();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        // Spring session lifecycle logic is a mess.  Ensure we only close a session once.
        if (this.closed.compareAndSet(false, true)) {
            Session<Void> requestSession = this.session;
            try (BatchContext context = this.resumeBatch()) {
                // If batch was discarded, close it
                if (this.batch.getState() == Batch.State.DISCARDED) {
                    this.batch.close();
                }
                // If batch is closed, close valid session in a new batch
                try (Batch batch = (this.batch.getState() == Batch.State.CLOSED) && requestSession.isValid() ? this.manager.getBatcher().createBatch() : this.batch) {
                    // Ensure session is closed, even if invalid
                    try (Session<Void> session = requestSession) {
                        if (session.isValid()) {
                            // According to §7.6 of the servlet specification:
                            // The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
                            session.getMetaData().setLastAccess(this.startTime, Instant.now());
                        }
                    }
                }
            } finally {
                // Switch to OOB session, in case this session is referenced outside the scope of this request
                this.session = new OOBSession<>(this.manager, requestSession.getId(), null);
            }
        }
    }

    private BatchContext resumeBatch() {
        B batch = (this.batch != null) && (this.batch.getState() != Batch.State.CLOSED) ? this.batch : null;
        return this.manager.getBatcher().resumeBatch(batch);
    }
}
