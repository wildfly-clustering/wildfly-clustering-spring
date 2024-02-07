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

package org.wildfly.clustering.spring.session.servlet;

import java.io.Serializable;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author Paul Ferraro
 */
public class MutableInteger implements IntSupplier, IntConsumer, Serializable {
	private static final long serialVersionUID = -5935940924708909645L;

	@ProtoField(value = 1, defaultValue = "0")
	volatile int value;

	@ProtoFactory
	public MutableInteger(int value) {
		this.value = value;
	}

	@Override
	public void accept(int value) {
		this.value = value;
	}

	@Override
	public int getAsInt() {
		return this.value;
	}
}
