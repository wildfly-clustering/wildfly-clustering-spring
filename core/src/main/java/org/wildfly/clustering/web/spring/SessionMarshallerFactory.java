/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.spring;

import java.util.EnumSet;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jdk.JavaByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.SimpleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.ValueExternalizer;
import org.wildfly.clustering.web.spring.security.SpringSecuritySerializationContextInitializerProvider;

/**
 * @author Paul Ferraro
 */
public enum SessionMarshallerFactory implements Function<ClassLoader, ByteBufferMarshaller> {

    JAVA() {
        @Override
        public ByteBufferMarshaller apply(ClassLoader loader) {
            return new JavaByteBufferMarshaller(new ValueExternalizer<>(loader));
        }
    },
    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(ClassLoader loader) {
            return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, loader), loader);
        }
    },
    PROTOSTREAM() {
        @Override
        public ByteBufferMarshaller apply(ClassLoader loader) {
            SerializationContextBuilder builder = new SerializationContextBuilder(new SimpleClassLoaderMarshaller(loader)).load(loader)
                    .register(EnumSet.allOf(SpringSecuritySerializationContextInitializerProvider.class))
                    ;
            return new ProtoStreamByteBufferMarshaller(builder.build());
        }
    },
    ;
}
