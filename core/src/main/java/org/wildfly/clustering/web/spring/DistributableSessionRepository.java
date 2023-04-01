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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * A session repository implementation based on a {@link SessionManager}.
 * Additionally indexes sessions using a set of {@link SSOManager} instances.
 * @author Paul Ferraro
 */
public class DistributableSessionRepository<B extends Batch> implements FindByIndexNameSessionRepository<SpringSession> {
    // Handle redundant calls to findById(...)
    private static final ThreadLocal<SpringSession> CURRENT_SESSION = new ThreadLocal<>();

    private final SessionManager<Void, B> manager;
    private final ApplicationEventPublisher publisher;
    private final BiConsumer<ImmutableSession, BiFunction<Object, org.springframework.session.Session, ApplicationEvent>> destroyAction;
    private final IndexingConfiguration<B> indexing;

    public DistributableSessionRepository(DistributableSessionRepositoryConfiguration<B> configuration) {
        this.manager = configuration.getSessionManager();
        this.publisher = configuration.getEventPublisher();
        this.destroyAction = configuration.getSessionDestroyAction();
        this.indexing = configuration.getIndexingConfiguration();
    }

    @Override
    public SpringSession createSession() {
        String id = this.manager.getIdentifierFactory().get();
        boolean close = true;
        Batcher<B> batcher = this.manager.getBatcher();
        B batch = batcher.createBatch();
        try {
            Session<Void> session = this.manager.createSession(id);
            B suspendedBatch = batcher.suspendBatch();
            DistributableSession<B> result = new DistributableSession<>(this.manager, session, suspendedBatch, this.indexing);
            this.publisher.publishEvent(new SessionCreatedEvent(this, result));
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
    public SpringSession findById(String id) {
        // Handle redundant calls to findById(...)
        SpringSession current = CURRENT_SESSION.get();
        if ((current != null) && current.getId().equals(id)) {
            return current;
        }
        boolean close = true;
        Batcher<B> batcher = this.manager.getBatcher();
        B batch = batcher.createBatch();
        try {
            Session<Void> session = this.manager.findSession(id);
            if (session == null) return null;
            B suspendedBatch = batcher.suspendBatch();
            DistributableSession<B> result = new DistributableSession<>(this.manager, session, suspendedBatch, this.indexing);
            close = false;
            CURRENT_SESSION.set(result);
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
        try (Session<Void> session = this.manager.findSession(id)) {
            if (session != null) {
                this.destroyAction.accept(session, SessionDestroyedEvent::new);
                session.invalidate();
            }
        } finally {
            CURRENT_SESSION.remove();
        }
    }

    @Override
    public void save(SpringSession session) {
        CURRENT_SESSION.remove();
        session.close();
    }

    @Override
    public Map<String, SpringSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
        Set<String> sessions = Collections.emptySet();
        SSOManager<Void, String, String, Void, B> manager = this.indexing.getSSOManagers().get(indexValue);
        if (manager != null) {
            try (Batch batch = manager.getBatcher().createBatch()) {
                SSO<Void, String, String, Void> sso = manager.findSSO(indexValue);
                if (sso != null) {
                    sessions = sso.getSessions().getDeployments();
                }
            }
        }
        if (!sessions.isEmpty()) {
            Map<String, SpringSession> result = new HashMap<>();
            try (Batch batch = this.manager.getBatcher().createBatch()) {
                for (String sessionId : sessions) {
                    ImmutableSession session = this.manager.readSession(sessionId);
                    if (session != null) {
                        result.put(sessionId, new DistributableImmutableSession(session));
                    }
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }
}
