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

package org.wildfly.clustering.web.spring.security.authentication;

import java.io.IOException;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class AuthenticationMarshaller<A extends Authentication> implements FieldSetMarshaller<A, AuthenticationTokenConfiguration> {

	private static final int PRINCIPAL_INDEX = 0;
	private static final int CREDENTIALS_INDEX = 1;
	private static final int GRANTED_AUTHORITY_INDEX = 2;
	private static final int DETAILS_INDEX = 3;
	private static final int FIELDS = 4;

	private final Function<AuthenticationTokenConfiguration, A> factory;

	AuthenticationMarshaller(Function<AuthenticationTokenConfiguration, A> factory) {
		this.factory = factory;
	}

	@Override
	public AuthenticationTokenConfiguration createInitialValue() {
		return new AuthenticationTokenConfiguration();
	}

	@Override
	public A build(AuthenticationTokenConfiguration config) {
		return this.factory.apply(config);
	}

	@Override
	public AuthenticationTokenConfiguration readFrom(ProtoStreamReader reader, int index, WireType type, AuthenticationTokenConfiguration config) throws IOException {
		switch (index) {
			case PRINCIPAL_INDEX:
				return config.setPrincipal(reader.readAny());
			case CREDENTIALS_INDEX:
				return config.setCredentials(reader.readAny());
			case GRANTED_AUTHORITY_INDEX:
				return config.addAuthority(reader.readAny(GrantedAuthority.class));
			case DETAILS_INDEX:
				return config.setDetails(reader.readAny());
			default:
				reader.skipField(type);
				return config;
		}
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, A token) throws IOException {
		Object principal = token.getPrincipal();
		if (principal != null) {
			writer.writeAny(PRINCIPAL_INDEX, token.getPrincipal());
		}
		Object credentials = token.getCredentials();
		if (!this.createInitialValue().getCredentials().equals(credentials)) {
			writer.writeAny(CREDENTIALS_INDEX, credentials);
		}
		for (GrantedAuthority authority : token.getAuthorities()) {
			writer.writeAny(GRANTED_AUTHORITY_INDEX, authority);
		}
		Object details = token.getDetails();
		if (details != null) {
			writer.writeAny(DETAILS_INDEX, details);
		}
	}

	@Override
	public int getFields() {
		return FIELDS;
	}
}
