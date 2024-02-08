/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session;

import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.wildfly.clustering.marshalling.jboss.LoadedClassTable;
import org.wildfly.clustering.marshalling.jboss.LoadedObjectTable;

/**
 * @author Paul Ferraro
 */
public enum JBossMarshallingVersion implements Function<ClassLoader, MarshallingConfiguration> {

	VERSION_1() {
		@Override
		public MarshallingConfiguration apply(ClassLoader loader) {
			MarshallingConfiguration config = new MarshallingConfiguration();
			config.setClassResolver(new SimpleClassResolver(loader));
			config.setClassTable(new LoadedClassTable(loader));
			config.setObjectTable(new LoadedObjectTable(loader));
			return config;
		}
	},
	;
	static final JBossMarshallingVersion CURRENT = VERSION_1;
}
