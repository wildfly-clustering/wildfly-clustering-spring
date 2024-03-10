/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.wildfly.clustering.session.ImmutableSession;

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
		return (T) this.session.getAttributes().get(attributeName);
	}

	@Override
	public Set<String> getAttributeNames() {
		return this.session.getAttributes().keySet();
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
		return this.session.getMetaData().getTimeout();
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
	public void invalidate() {
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
