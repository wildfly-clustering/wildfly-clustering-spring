/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * @author Paul Ferraro
 */
public class SessionManagementArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		Stream.Builder<Arguments> builder = Stream.builder();
		for (SessionPersistenceGranularity strategy : EnumSet.allOf(SessionPersistenceGranularity.class)) {
			for (SessionAttributeMarshaller marshaller : EnumSet.allOf(SessionAttributeMarshaller.class)) {
				builder.add(Arguments.of(new SessionManagementParameters() {
					@Override
					public SessionPersistenceGranularity getSessionPersistenceGranularity() {
						return strategy;
					}

					@Override
					public SessionAttributeMarshaller getSessionMarshallerFactory() {
						return marshaller;
					}

					@Override
					public String toString() {
						return Map.of("granularity", strategy, "marshaller", marshaller).toString();
					}
				}));
			}
		}
		return builder.build();
	}
}
