/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.server.util.Reference;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.ImmutableSessionMetaData;

/**
 * A Spring Session facade for an immutable session.
 * @author Paul Ferraro
 * @param <S> the session type
 */
public class DistributableImmutableSession<S extends ImmutableSession> implements SpringSession {
	private static final Predicate<ImmutableSessionMetaData> EXPIRED = ImmutableSessionMetaData::isExpired;

	private final Reference.Reader<S> reader;
	private final Reference.Reader<ImmutableSessionMetaData> metaDataReader;
	private final Reference.Reader<Map<String, Object>> attributesReader;

	/**
	 * Creates a Spring Session facade for an immutable session.
	 * @param reference an immutable session reference
	 */
	public DistributableImmutableSession(Reference<S> reference) {
		this.reader = reference.getReader();
		this.metaDataReader = this.reader.map(ImmutableSession.METADATA);
		this.attributesReader = this.reader.map(ImmutableSession.ATTRIBUTES);
	}

	@Override
	public String getId() {
		return this.reader.map(ImmutableSession.IDENTIFIER).get();
	}

	@Override
	public String changeSessionId() {
		return this.getId();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAttribute(String attributeName) {
		return (T) this.attributesReader.map(ImmutableSession.GET_ATTRIBUTE.composeUnary(Function.identity(), Function.of(attributeName))).get();
	}

	@Override
	public Set<String> getAttributeNames() {
		return this.attributesReader.map(ImmutableSession.ATTRIBUTE_NAMES).get();
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
		return this.metaDataReader.map(ImmutableSessionMetaData.CREATION_TIME).get();
	}

	@Override
	public void setLastAccessedTime(Instant lastAccessedTime) {
		// Do nothing
	}

	@Override
	public Instant getLastAccessedTime() {
		return this.metaDataReader.map(ImmutableSessionMetaData.LAST_ACCESS_TIME).get();
	}

	@Override
	public void setMaxInactiveInterval(Duration interval) {
		// Do nothing
	}

	@Override
	public Duration getMaxInactiveInterval() {
		return this.metaDataReader.map(ImmutableSessionMetaData.MAX_IDLE).get().orElse(Duration.ZERO);
	}

	@Override
	public boolean isExpired() {
		return this.metaDataReader.map(EXPIRED.thenBox()).get();
	}

	@Override
	public boolean isNew() {
		return this.metaDataReader.map(ImmutableSessionMetaData.LAST_ACCESS_START_TIME).get().isEmpty();
	}

	@Override
	public void invalidate() {
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
