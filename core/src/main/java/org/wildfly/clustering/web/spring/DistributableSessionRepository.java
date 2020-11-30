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
import java.util.Optional;
import java.util.function.Consumer;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSessionRepository<B extends Batch> implements SessionRepository<DistributableSession<B>> {
    // Workaround for https://github.com/spring-projects/spring-session/issues/1731
    @SuppressWarnings("rawtypes")
    private static final ThreadLocal<DistributableSession> CURRENT_SESSION = new ThreadLocal<>();

    private final SessionManager<Void, B> manager;
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
    public DistributableSession<B> findById(String id) {
        // Ugly workaround for bizarre behavior in org.springframework.session.web.http.SessionRepositoryFilter.SessionRepositoryRequestWrapper.commitSession()
        // that triggers an extraneous SessionRepository.findById(...) for a recently saved requested session.
        DistributableSession<B> current = CURRENT_SESSION.get();
        if (current != null) {
            CURRENT_SESSION.remove();
            return current;
        }
        // Workaround for WFLY-14118
        synchronized (this) {
            boolean close = true;
            Batcher<B> batcher = this.manager.getBatcher();
            B batch = batcher.createBatch();
            try {
                Session<Void> session = this.manager.findSession(id);
                if (session == null) return null;
                B suspendedBatch = batcher.suspendBatch();
                DistributableSession<B> result = new DistributableSession<>(this.manager, session, suspendedBatch);
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
    }

    @Override
    public void deleteById(String id) {
        try (Session<Void> session = this.manager.findSession(id)) {
            if (session != null) {
                this.destroyAction.accept(session);
                session.invalidate();
            }
        } finally {
            // Remove thread local here - as there will not be an extraneous call to SessionRepository.findById(...)
            CURRENT_SESSION.remove();
        }
    }

    @Override
    public void save(DistributableSession<B> session) {
        session.close();
    }
}
