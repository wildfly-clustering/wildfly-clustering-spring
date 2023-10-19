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
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.security.PrivilegedActionException;
import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.ParametricPrivilegedExceptionAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class HashAuthenticationTokenMarshaller<T extends AbstractAuthenticationToken> implements ProtoStreamMarshaller<T>, ParametricPrivilegedExceptionAction<T, Map.Entry<Integer, Map.Entry<Object, Collection<GrantedAuthority>>>> {

	private static final int HASH_INDEX = 1;
	private static final int TOKEN_INDEX = 2;

	private static final int DEFAULT_HASH = 0;

	private final Class<T> tokenClass;
	private final ToIntFunction<T> hash;

	public HashAuthenticationTokenMarshaller(Class<T> tokenClass, ToIntFunction<T> hash) {
		this.tokenClass = tokenClass;
		this.hash = hash;
	}

	@Override
	public T readFrom(ProtoStreamReader reader) throws IOException {
		AtomicInteger hash = new AtomicInteger(DEFAULT_HASH);
		FieldSetMarshaller<T, AuthenticationTokenConfiguration> marshaller = this.createMarshaller(hash);
		FieldSetReader<AuthenticationTokenConfiguration> tokenReader = reader.createFieldSetReader(marshaller, TOKEN_INDEX);
		AuthenticationTokenConfiguration config = marshaller.createInitialValue();
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			int index = WireType.getTagFieldNumber(tag);
			if (index == HASH_INDEX) {
				hash.setPlain(reader.readSFixed32());
			} else if (tokenReader.contains(index)) {
				config = tokenReader.readField(config);
			} else {
				reader.skipField(tag);
			}
		}
		return marshaller.build(config);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, T token) throws IOException {
		int hash = this.hash.applyAsInt(token);
		if (hash != DEFAULT_HASH) {
			writer.writeSFixed32(HASH_INDEX, hash);
		}
		writer.createFieldSetWriter(this.createMarshaller(new AtomicInteger(hash)), TOKEN_INDEX).writeFields(token);
	}

	@Override
	public T run(Map.Entry<Integer, Map.Entry<Object, Collection<GrantedAuthority>>> parameter) throws Exception {
		// The constructor we need is private.
		Constructor<T> constructor = this.tokenClass.getDeclaredConstructor(Integer.class, Object.class, Collection.class);
		constructor.setAccessible(true);
		return constructor.newInstance(parameter.getKey(), parameter.getValue().getKey(), parameter.getValue().getValue());
	}

	@Override
	public Class<? extends T> getJavaClass() {
		return this.tokenClass;
	}

	private FieldSetMarshaller<T, AuthenticationTokenConfiguration> createMarshaller(AtomicInteger hash) {
		return new AuthenticationMarshaller<>(new Function<>() {
			@Override
			public T apply(AuthenticationTokenConfiguration config) {
				Object principal = config.getPrincipal();
				Collection<GrantedAuthority> authorities = config.getAuthorities();
				try {
					T token = WildFlySecurityManager.doUnchecked(new SimpleImmutableEntry<>(hash.getPlain(), new SimpleImmutableEntry<>(principal, authorities)), HashAuthenticationTokenMarshaller.this);
					token.setDetails(config.getDetails());
					return token;
				} catch (PrivilegedActionException e) {
					throw new UncheckedIOException(new IOException(e.getException()));
				}
			}
		});
	}
}
