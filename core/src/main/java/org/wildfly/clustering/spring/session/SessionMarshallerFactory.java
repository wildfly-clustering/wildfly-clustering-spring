/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import static org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder.newInstance;

import java.util.function.Function;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.Serializer;
import org.wildfly.clustering.marshalling.java.JavaByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.spring.security.SpringSecuritySerializationContextInitializer;
import org.wildfly.clustering.spring.security.authentication.SpringSecurityAuthenticationSerializationContextInitializer;
import org.wildfly.clustering.spring.security.authentication.jaas.SpringSecurityJaasAuthenticationSerializationContextInitializer;
import org.wildfly.clustering.spring.security.core.authority.SpringSecurityAuthoritySerializationContextInitializer;
import org.wildfly.clustering.spring.security.core.context.SpringSecurityContextSerializationContextInitializer;
import org.wildfly.clustering.spring.security.core.userdetails.SpringSecurityUserDetailsSerializationContextInitializer;
import org.wildfly.clustering.spring.security.web.authentication.SpringSecurityWebAuthenticationSerializationContextInitializer;
import org.wildfly.clustering.spring.security.web.authentication.preauth.SpringSecurityWebPreAuthSerializationContextInitializer;
import org.wildfly.clustering.spring.security.web.savedrequest.SpringSecurityWebSavedRequestSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public enum SessionMarshallerFactory implements Function<ClassLoader, ByteBufferMarshaller> {

	JAVA() {
		@Override
		public ByteBufferMarshaller apply(ClassLoader loader) {
			return new JavaByteBufferMarshaller(Serializer.of(loader));
		}
	},
	JBOSS() {
		@Override
		public ByteBufferMarshaller apply(ClassLoader loader) {
			return new JBossByteBufferMarshaller(MarshallingConfigurationRepository.from(JBossMarshallingVersion.CURRENT, loader), loader);
		}
	},
	PROTOSTREAM() {
		@Override
		public ByteBufferMarshaller apply(ClassLoader loader) {
			SerializationContextBuilder builder = newInstance(ClassLoaderMarshaller.of(loader))
					.load(loader)
					.register(new SpringSecuritySerializationContextInitializer())
					.register(new SpringSecurityAuthenticationSerializationContextInitializer())
					.register(new SpringSecurityJaasAuthenticationSerializationContextInitializer())
					.register(new SpringSecurityAuthoritySerializationContextInitializer())
					.register(new SpringSecurityContextSerializationContextInitializer())
					.register(new SpringSecurityUserDetailsSerializationContextInitializer())
					.register(new SpringSecurityWebAuthenticationSerializationContextInitializer())
					.register(new SpringSecurityWebPreAuthSerializationContextInitializer())
					.register(new SpringSecurityWebSavedRequestSerializationContextInitializer())
					;
			return new ProtoStreamByteBufferMarshaller(builder.build());
		}
	},
	;
}
