/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication.jaas;

import java.io.IOException;
import java.security.Principal;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * A ProtoStream marshaller for a JAAS granted authority
 * @author Paul Ferraro
 */
public enum JaasGrantedAuthorityMarshaller implements ProtoStreamMarshaller<JaasGrantedAuthority> {
	/** Singleton instance */
	INSTANCE;

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
