/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context;

import java.io.ObjectInputFilter;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.Serializer;
import org.wildfly.clustering.marshalling.java.JavaByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * @author Paul Ferraro
 */
public enum SessionMarshallerFactory implements Function<Map.Entry<Environment, ResourceLoader>, ByteBufferMarshaller> {

	JAVA() {
		@Override
		public ByteBufferMarshaller apply(Map.Entry<Environment, ResourceLoader> context) {
			Environment environment = context.getKey();
			ObjectInputFilter filter = Optional.ofNullable(environment.getProperty("jdk.serialFilter")).map(ObjectInputFilter.Config::createFilter).orElse(null);
			return new JavaByteBufferMarshaller(Serializer.of(context.getValue().getClassLoader()), filter);
		}
	},
	JBOSS() {
		@Override
		public ByteBufferMarshaller apply(Map.Entry<Environment, ResourceLoader> context) {
			ClassLoader loader = context.getValue().getClassLoader();
			return new JBossByteBufferMarshaller(MarshallingConfigurationRepository.from(JBossMarshallingVersion.CURRENT, loader), loader);
		}
	},
	PROTOSTREAM() {
		@Override
		public ByteBufferMarshaller apply(Map.Entry<Environment, ResourceLoader> context) {
			ClassLoader loader = context.getValue().getClassLoader();
			SerializationContextBuilder builder = SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(loader)).load(loader);
			return new ProtoStreamByteBufferMarshaller(builder.build());
		}
	},
	;
}
