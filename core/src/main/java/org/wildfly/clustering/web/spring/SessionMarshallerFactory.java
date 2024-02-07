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

import java.util.function.Function;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.SessionMarshallerFactory} instead.
 */
@Deprecated(forRemoval = true)
public enum SessionMarshallerFactory implements Function<ClassLoader, ByteBufferMarshaller> {

	JAVA(org.wildfly.clustering.spring.session.SessionMarshallerFactory.JAVA),
	JBOSS(org.wildfly.clustering.spring.session.SessionMarshallerFactory.JBOSS),
	PROTOSTREAM(org.wildfly.clustering.spring.session.SessionMarshallerFactory.PROTOSTREAM),
	;
	private final Function<ClassLoader, ByteBufferMarshaller> factory;

	SessionMarshallerFactory(Function<ClassLoader, ByteBufferMarshaller> factory) {
		this.factory = factory;
	}

	@Override
	public ByteBufferMarshaller apply(ClassLoader loader) {
		return this.factory.apply(loader);
	}
}
