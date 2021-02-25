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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Immutable session implementation for use by {@link org.springframework.session.FindByIndexNameSessionRepository#findByIndexNameAndIndexValue(String, String)}.
 * @author Paul Ferraro
 */
public class DistributableImmutableSession implements SpringSession {

    private final ImmutableSession session;

    public DistributableImmutableSession(ImmutableSession session) {
        this.session = session;
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String attributeName) {
        return (T) this.session.getAttributes().getAttribute(attributeName);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.session.getAttributes().getAttributeNames();
    }

    @Override
    public void setAttribute(String attributeName, Object attributeValue) {
        // Do nothing
    }

    @Override
    public void removeAttribute(String attributeName) {
        // Do nothing
    }

    @Override
    public Instant getCreationTime() {
        return this.session.getMetaData().getCreationTime();
    }

    @Override
    public void setLastAccessedTime(Instant lastAccessedTime) {
        // Do nothing
    }

    @Override
    public Instant getLastAccessedTime() {
        return this.session.getMetaData().getLastAccessStartTime();
    }

    @Override
    public void setMaxInactiveInterval(Duration interval) {
        // Do nothing
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return this.session.getMetaData().getMaxInactiveInterval();
    }

    @Override
    public boolean isExpired() {
        return this.session.getMetaData().isExpired();
    }

    @Override
    public boolean isNew() {
        return this.session.getMetaData().isNew();
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
