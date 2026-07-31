/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context;

import java.io.ObjectInputFilter;
import java.util.Optional;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.UnmarshallingObjectInputFilter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.java.JavaByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderResolver;
import org.wildfly.clustering.marshalling.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamConfiguration;

/**
 * Enumerates the default set of available session attribute marshallers.
 * @author Paul Ferraro
 */
public enum SessionAttributeMarshaller implements BiFunction<Environment, ResourceLoader, ByteBufferMarshaller> {
	/** A JDK serialization marshaller */
	JAVA() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			return new JavaByteBufferMarshaller(loader.getClassLoader(), this.inputFilter(environment));
		}
	},
	/** A JBoss Marshalling marshaller */
	JBOSS() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			ClassLoader classLoader = loader.getClassLoader();
			MarshallingConfiguration configuration = MarshallingConfigurationBuilder.newInstance(new SimpleClassResolver(classLoader)).load(classLoader).build();
			this.serialFilter(environment).map(UnmarshallingObjectInputFilter.Factory::createFilter).ifPresent(configuration::setUnmarshallingFilter);
			return new JBossByteBufferMarshaller(configuration, classLoader);
		}
	},
	/** A ProtoStream marshaller */
	PROTOSTREAM() {
		@Override
		public ByteBufferMarshaller apply(Environment environment, ResourceLoader loader) {
			return new ProtoStreamByteBufferMarshaller(ImmutableSerializationContext.Builder.with(ProtoStreamConfiguration.Builder.with(ClassLoaderResolver.of(loader.getClassLoader())).withObjectInputFilter(this.inputFilter(environment)).build()).build());
		}
	},
	;

	ObjectInputFilter inputFilter(Environment environment) {
		return this.serialFilter(environment).map(ObjectInputFilter.Config::createFilter).orElse(ObjectInputFilter.Config.getSerialFilter());
	}

	Optional<String> serialFilter(Environment environment) {
		return Optional.ofNullable(environment.getProperty("jdk.serialFilter"));
	}
}
