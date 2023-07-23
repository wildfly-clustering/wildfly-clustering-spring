/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.spring.security.web.savedrequest;

import java.io.IOException;

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
	private static final int COMMENT_INDEX = 4;
	private static final int DOMAIN_INDEX = 5;
	private static final int MAX_AGE_INDEX = 6;
	private static final int PATH_INDEX = 7;
	private static final int VERSION_INDEX = 8;

	private static final int DEFAULT_MAX_AGE = -1;
	private static final int DEFAULT_VERSION = 0;

	@Override
	public SavedCookie readFrom(ProtoStreamReader reader) throws IOException {
		String name = null;
		String value = null;
		String comment = null;
		String domain = null;
		int maxAge = DEFAULT_MAX_AGE;
		String path = null;
		boolean secure = false;
		int version = DEFAULT_VERSION;
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			switch (WireType.getTagFieldNumber(tag)) {
				case SECURE_NAME_INDEX:
					secure = true;
				case NAME_INDEX:
					name = reader.readString();
					break;
				case VALUE_INDEX:
					value = reader.readString();
					break;
				case COMMENT_INDEX:
					comment = reader.readString();
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
				case VERSION_INDEX:
					version = reader.readUInt32();
					break;
				default:
					reader.skipField(tag);
			}
		}
		return new SavedCookie(name, value, comment, domain, maxAge, path, secure, version);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, SavedCookie cookie) throws IOException {
		String name = cookie.getName();
		writer.writeString(cookie.isSecure() ? SECURE_NAME_INDEX : NAME_INDEX, name);
		String value = cookie.getValue();
		if (value != null) {
			writer.writeString(VALUE_INDEX, value);
		}
		String comment = cookie.getComment();
		if (comment != null) {
			writer.writeString(COMMENT_INDEX, comment);
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
		int version = cookie.getVersion();
		if (version != DEFAULT_VERSION) {
			writer.writeUInt32(VERSION_INDEX, version);
		}
	}

	@Override
	public Class<? extends SavedCookie> getJavaClass() {
		return SavedCookie.class;
	}
}
