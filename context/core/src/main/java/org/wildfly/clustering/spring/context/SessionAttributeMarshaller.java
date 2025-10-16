/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context;

import java.io.ObjectInputFilter;
import java.util.Optional;
import java.util.function.BiFunction;

import org.jboss.marshalling.SimpleClassResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.java.JavaByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * Enumerates the default set of available session attribute marshallers.
 * @author Paul Ferraro
 */
public enum SessionAttributeMarshaller implements BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> {
	/** A JDK serialization marshaller */
	JAVA() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ClassLoader classLoader = loader.getClassLoader();
			ObjectInputFilter filter = Optional.ofNullable(environment.getProperty("jdk.serialFilter")).map(ObjectInputFilter.Config::createFilter).orElse(null);
			return new JavaByteBufferMarshaller(classLoader, filter);
		}
	},
	/** A JBoss Marshalling marshaller */
	JBOSS() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ClassLoader classLoader = loader.getClassLoader();
			return new JBossByteBufferMarshaller(MarshallingConfigurationBuilder.newInstance(new SimpleClassResolver(classLoader)).load(classLoader).build(), classLoader);
		}
	},
	/** A ProtoStream marshaller */
	PROTOSTREAM() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ClassLoader classLoader = loader.getClassLoader();
			return new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(classLoader)).load(classLoader).build());
		}
	},
	;
}
