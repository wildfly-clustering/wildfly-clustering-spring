/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
