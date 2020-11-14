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
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSessionRepository<B extends Batch> implements SessionRepository<DistributableSession<B>> {

    private final SessionManager<Void, B> manager;
    // Needed to obtain reference to session for use by deleteById(...)
    private final Map<String, Map.Entry<Session<Void>, B>> sessions = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher publisher;
    private final Optional<Duration> defaultTimeout;
    private final Consumer<ImmutableSession> destroyAction;

    public DistributableSessionRepository(SessionManager<Void, B> manager, Optional<Duration> defaultTimeout, ApplicationEventPublisher publisher, ServletContext context) {
        this.manager = manager;
        this.defaultTimeout = defaultTimeout;
        this.publisher = publisher;
        this.destroyAction = new ImmutableSessionDestroyAction(publisher, SessionDestroyedEvent::new, context);
    }

    @Override
    public DistributableSession<B> createSession() {
        String id = this.manager.createIdentifier();
        boolean close = true;
        Batcher<B> batcher = this.manager.getBatcher();
        B batch = batcher.createBatch();
        try {
            Session<Void> session = this.manager.createSession(id);
            // This is present for servlet version < 4.0
            if (this.defaultTimeout.isPresent()) {
                session.getMetaData().setMaxInactiveInterval(this.defaultTimeout.get());
            }
            B suspendedBatch = batcher.suspendBatch();
            DistributableSession<B> result = new DistributableSession<>(this.manager, session, suspendedBatch);
            this.sessions.put(id, new AbstractMap.SimpleImmutableEntry<>(session, suspendedBatch));
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
            B suspendedBatch = batcher.suspendBatch();
            DistributableSession<B> result = new DistributableSession<>(this.manager, session, suspendedBatch);
            this.sessions.put(id, new AbstractMap.SimpleImmutableEntry<>(session, suspendedBatch));
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
        Map.Entry<Session<Void>, B> entry = this.sessions.remove(id);
        if (entry != null) {
            Session<Void> session = entry.getKey();
            this.destroyAction.accept(session);
            try (BatchContext context = this.manager.getBatcher().resumeBatch(entry.getValue())) {
                session.invalidate();
            }
        }
    }

    @Override
    public void save(DistributableSession<B> session) {
        Map.Entry<Session<Void>, B> entry = this.sessions.remove(session.getId());
        if (entry != null) {
            try (Session<Void> closeable = entry.getKey()) {
                // Do nothing
            }
        }
    }
}
