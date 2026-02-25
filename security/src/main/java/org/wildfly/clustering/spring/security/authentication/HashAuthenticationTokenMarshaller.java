/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.authentication;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
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

/**
 * A ProtoStream marshaller for a hash authentication token.
 * @author Paul Ferraro
 * @param <T> token type
 */
public class HashAuthenticationTokenMarshaller<T extends AbstractAuthenticationToken> implements ProtoStreamMarshaller<T> {

	private static final int HASH_INDEX = 1;
	private static final int TOKEN_INDEX = 2;

	private static final int DEFAULT_HASH = 0;

	private final MethodHandle constructorHandle;
	private final Class<T> tokenClass;
	private final ToIntFunction<T> hash;

	HashAuthenticationTokenMarshaller(Class<T> tokenClass, ToIntFunction<T> hash) {
		this.tokenClass = tokenClass;
		try {
			this.constructorHandle = MethodHandles.privateLookupIn(tokenClass, MethodHandles.lookup()).findConstructor(tokenClass, MethodType.methodType(void.class, Integer.class, Object.class, Collection.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
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

	T createToken(Class<T> tokenClass, Integer hash, Object principal, Collection<GrantedAuthority> authorities) throws Exception {
		try {
			return tokenClass.cast(this.constructorHandle.invoke(hash, principal, authorities));
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Class<? extends T> getJavaClass() {
		return this.tokenClass;
	}

	private FieldSetMarshaller<T, AuthenticationTokenConfiguration> createMarshaller(AtomicInteger hash) {
		Class<T> tokenClass = this.tokenClass;
		return new AuthenticationMarshaller<>(new Function<>() {
			@Override
			public T apply(AuthenticationTokenConfiguration config) {
				Object principal = config.getPrincipal();
				Collection<GrantedAuthority> authorities = config.getAuthorities();
				try {
					T token = HashAuthenticationTokenMarshaller.this.createToken(tokenClass, hash.getPlain(), principal, authorities);
					token.setDetails(config.getDetails());
					return token;
				} catch (Exception e) {
					throw new UncheckedIOException(new IOException(e));
				}
			}
		});
	}
}
