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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSessionRepository<B extends Batch> implements SessionRepository<DistributableSession<B>> {

    private final SessionManager<Void, B> manager;
    // Needed to obtain reference to session for use by deleteById(...)
    private final Map<String, Session<Void>> sessions = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher publisher;
    private final Optional<Duration> defaultTimeout;

    public DistributableSessionRepository(SessionManager<Void, B> manager, Optional<Duration> defaultTimeout, ApplicationEventPublisher publisher) {
        this.manager = manager;
        this.defaultTimeout = defaultTimeout;
        this.publisher = publisher;
    }

    @Override
    public DistributableSession<B> createSession() {
        String id = this.manager.createIdentifier();
        boolean close = true;
        Batcher<B> batcher = this.manager.getBatcher();
        B batch = batcher.createBatch();
        try {
            Session<Void> session = this.manager.createSession(id);
            this.sessions.put(id, session);
            // This is present for servlet version < 4.0
            if (this.defaultTimeout.isPresent()) {
                session.getMetaData().setMaxInactiveInterval(this.defaultTimeout.get());
            }
            DistributableSession<B> result = new DistributableSession<>(this.manager, session, batcher.suspendBatch());
            this.publisher.publishEvent(new SessionCreatedEvent(this, result));
            close = false;
            return result;
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        } finally {
            if (close) {
                batch.close();
                this.sessions.remove(id);
            }
        }
    }

    @Override
    public DistributableSession<B> findById(String id) {
        boolean close = true;
        Batcher<B> batcher = this.manager.getBatcher();
        B batch = batcher.createBatch();
        try {
            Session<Void> session = this.manager.findSession(id);
            if (session == null) return null;
            this.sessions.put(id, session);
            DistributableSession<B> result = new DistributableSession<>(this.manager, session, batcher.suspendBatch());
            close = false;
            return result;
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        } finally {
            if (close) {
                batch.close();
            }
        }
    }

    @Override
    public void deleteById(String id) {
        Session<Void> session = this.sessions.remove(id);
        if (session != null) {
            session.invalidate();
        } else {
            Batcher<B> batcher = this.manager.getBatcher();
            try (B batch = batcher.createBatch()) {
                session = this.manager.findSession(id);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
    }

    @Override
    public void save(DistributableSession<B> session) {
        try (Session<Void> closeable = this.sessions.remove(session.getId())) {
            // Do nothing
        }
    }
}
