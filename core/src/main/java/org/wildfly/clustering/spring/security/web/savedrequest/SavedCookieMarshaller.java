/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.savedrequest;

import java.io.IOException;

import jakarta.servlet.http.Cookie;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.web.savedrequest.SavedCookie;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SavedCookieMarshaller implements ProtoStreamMarshaller<SavedCookie> {
	private static final int NAME_INDEX = 1;
	private static final int SECURE_NAME_INDEX = 2;
	private static final int VALUE_INDEX = 3;
	private static final int HTTP_ONLY_VALUE_INDEX = 4;
	private static final int DOMAIN_INDEX = 5;
	private static final int MAX_AGE_INDEX = 6;
	private static final int PATH_INDEX = 7;

	private static final int DEFAULT_MAX_AGE = -1;

	@Override
	public SavedCookie readFrom(ProtoStreamReader reader) throws IOException {
		String name = null;
		String value = null;
		boolean secure = false;
		boolean httpOnly = false;
		String domain = null;
		int maxAge = DEFAULT_MAX_AGE;
		String path = null;
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			switch (WireType.getTagFieldNumber(tag)) {
				case SECURE_NAME_INDEX:
					secure = true;
				case NAME_INDEX:
					name = reader.readString();
					break;
				case HTTP_ONLY_VALUE_INDEX:
					httpOnly = true;
				case VALUE_INDEX:
					value = reader.readString();
					break;
				case DOMAIN_INDEX:
					domain = reader.readString();
					break;
				case MAX_AGE_INDEX:
					maxAge = reader.readUInt32();
					break;
				case PATH_INDEX:
					path = reader.readString();
					break;
				default:
					reader.skipField(tag);
			}
		}
		Cookie cookie = new Cookie(name, value);
		cookie.setDomain(domain);
		cookie.setHttpOnly(httpOnly);
		cookie.setMaxAge(maxAge);
		cookie.setPath(path);
		cookie.setSecure(secure);
		return new SavedCookie(cookie);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, SavedCookie cookie) throws IOException {
		String name = cookie.getName();
		writer.writeString(cookie.isSecure() ? SECURE_NAME_INDEX : NAME_INDEX, name);
		String value = cookie.getValue();
		if (value != null) {
			writer.writeString(VALUE_INDEX, value);
		}
		String domain = cookie.getDomain();
		if (domain != null) {
			writer.writeString(DOMAIN_INDEX, domain);
		}
		int maxAge = cookie.getMaxAge();
		if (maxAge != DEFAULT_MAX_AGE) {
			writer.writeUInt32(MAX_AGE_INDEX, maxAge);
		}
		String path = cookie.getPath();
		if (path != null) {
			writer.writeString(PATH_INDEX, path);
		}
	}

	@Override
	public Class<? extends SavedCookie> getJavaClass() {
		return SavedCookie.class;
	}
}
