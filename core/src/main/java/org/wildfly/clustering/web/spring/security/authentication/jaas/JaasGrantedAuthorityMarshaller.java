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

package org.wildfly.clustering.web.spring.security.authentication.jaas;

import java.io.IOException;
import java.security.Principal;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class JaasGrantedAuthorityMarshaller implements ProtoStreamMarshaller<JaasGrantedAuthority> {

	private static final int ROLE_INDEX = 1;
	private static final int PRINCIPAL_INDEX = 2;

	@Override
	public JaasGrantedAuthority readFrom(ProtoStreamReader reader) throws IOException {
		Principal principal = null;
		String role = null;
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			switch (WireType.getTagFieldNumber(tag)) {
				case ROLE_INDEX:
					role = reader.readString();
					break;
				case PRINCIPAL_INDEX:
					principal = reader.readAny(Principal.class);
					break;
				default:
					reader.skipField(tag);
			}
		}
		return new JaasGrantedAuthority(role, principal);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, JaasGrantedAuthority authority) throws IOException {
		String role = authority.getAuthority();
		if (role != null) {
			writer.writeString(ROLE_INDEX, role);
		}
		Principal principal = authority.getPrincipal();
		if (principal != null) {
			writer.writeAny(PRINCIPAL_INDEX, principal);
		}
	}

	@Override
	public Class<? extends JaasGrantedAuthority> getJavaClass() {
		return JaasGrantedAuthority.class;
	}
}
