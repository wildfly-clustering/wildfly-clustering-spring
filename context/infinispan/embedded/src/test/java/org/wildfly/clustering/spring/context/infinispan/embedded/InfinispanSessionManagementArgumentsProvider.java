/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.wildfly.clustering.spring.context.SessionAttributeMarshaller;
import org.wildfly.clustering.spring.context.SessionPersistenceGranularity;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementArgumentsProvider implements ArgumentsProvider {

	private final Set<String> templates = Set.of("dist-non-tx", "dist-tx", "repl-non-tx", "repl-tx");

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
		Stream.Builder<Arguments> builder = Stream.builder();
		for (String template : this.templates) {
			for (SessionPersistenceGranularity strategy : EnumSet.allOf(SessionPersistenceGranularity.class)) {
				for (SessionAttributeMarshaller marshaller : EnumSet.allOf(SessionAttributeMarshaller.class)) {
					builder.add(Arguments.of(new InfinispanSessionManagementParameters() {
						@Override
						public SessionPersistenceGranularity getSessionPersistenceGranularity() {
							return strategy;
						}

						@Override
						public SessionAttributeMarshaller getSessionMarshallerFactory() {
							return marshaller;
						}

						@Override
						public String getTemplate() {
							return template;
						}

						@Override
						public String toString() {
							return Map.of("template", template, "granularity", strategy, "marshaller", marshaller).toString();
						}
					}));
				}
			}
		}
		return builder.build();
	}
}
