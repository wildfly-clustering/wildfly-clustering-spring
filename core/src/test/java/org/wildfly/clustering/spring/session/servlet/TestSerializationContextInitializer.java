/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * @author Paul Ferraro
 */
@AutoProtoSchemaBuilder(includeClasses = { MutableInteger.class })
public interface TestSerializationContextInitializer extends SerializationContextInitializer {

}
