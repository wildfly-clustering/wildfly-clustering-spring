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

package org.wildfly.clustering.spring.security;

import java.util.List;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.ByteBufferTestMarshaller;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.java.JavaTesterFactory;
import org.wildfly.clustering.spring.session.SessionMarshallerFactory;

/**
 * @author Paul Ferraro
 */
public enum ProtoStreamTesterFactory implements MarshallingTesterFactory {
	INSTANCE;

	private final ByteBufferMarshaller marshaller = SessionMarshallerFactory.PROTOSTREAM.apply(Thread.currentThread().getContextClassLoader());

	@Override
	public <T> MarshallingTester<T> createTester() {
		return new MarshallingTester<>(new ByteBufferTestMarshaller<>(this.marshaller), List.of(JavaTesterFactory.INSTANCE.getMarshaller()));
	}

	@Override
	public ByteBufferMarshaller getMarshaller() {
		return this.marshaller;
	}
}
