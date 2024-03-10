/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context;

import java.io.ObjectInputFilter;
import java.util.Optional;
import java.util.function.BiFunction;

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
public enum SessionAttributeMarshaller implements BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> {

	JAVA() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ObjectInputFilter filter = Optional.ofNullable(environment.getProperty("jdk.serialFilter")).map(ObjectInputFilter.Config::createFilter).orElse(null);
			return new JavaByteBufferMarshaller(Serializer.of(loader.getClassLoader()), filter);
		}
	},
	JBOSS() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ClassLoader classLoader = loader.getClassLoader();
			return new JBossByteBufferMarshaller(MarshallingConfigurationRepository.from(JBossMarshallingVersion.CURRENT, classLoader), classLoader);
		}
	},
	PROTOSTREAM() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ClassLoader classLoader = loader.getClassLoader();
			SerializationContextBuilder builder = SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(classLoader)).load(classLoader);
			return new ProtoStreamByteBufferMarshaller(builder.build());
		}
	},
	;
}
