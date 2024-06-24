/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context;

import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;

/**
 * @author Paul Ferraro
 */
public enum JBossMarshallingVersion implements Function<ClassLoader, MarshallingConfiguration> {

	VERSION_1() {
		@Override
		public MarshallingConfiguration apply(ClassLoader loader) {
			return MarshallingConfigurationBuilder.newInstance(new SimpleClassResolver(loader)).load(loader).build();
		}
	},
	;
	static final JBossMarshallingVersion CURRENT = VERSION_1;
}
