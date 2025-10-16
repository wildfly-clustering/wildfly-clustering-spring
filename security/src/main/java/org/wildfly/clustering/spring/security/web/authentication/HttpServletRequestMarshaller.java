/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * A marshaller of the fields of a {@link HttpServletRequest}.
 * @author Paul Ferraro
 */
public enum HttpServletRequestMarshaller implements FieldSetMarshaller.Supplied<HttpServletRequest, HttpServletRequestBuilder> {
	/** Singleton instance */
	INSTANCE;

	private static final int REMOTE_ADDRESS_INDEX = 0;
	private static final int SESSION_ID_INDEX = 1;
	private static final int FIELDS = 2;

	@Override
	public HttpServletRequestBuilder createInitialValue() {
		return new HttpServletRequestBuilder();
	}

	@Override
	public HttpServletRequestBuilder readFrom(ProtoStreamReader reader, int index, WireType type, HttpServletRequestBuilder builder) throws IOException {
		switch (index) {
			case REMOTE_ADDRESS_INDEX:
				return builder.setRemoteAddress(reader.readString());
			case SESSION_ID_INDEX:
				return builder.setSessionId(reader.readString());
			default:
				reader.skipField(type);
				return builder;
		}
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, HttpServletRequest request) throws IOException {
		String remoteAddress = request.getRemoteAddr();
		if (remoteAddress != null) {
			writer.writeString(REMOTE_ADDRESS_INDEX, remoteAddress);
		}
		HttpSession session = request.getSession(false);
		if (session != null) {
			writer.writeString(SESSION_ID_INDEX, session.getId());
		}
	}

	@Override
	public int getFields() {
		return FIELDS;
	}
}
