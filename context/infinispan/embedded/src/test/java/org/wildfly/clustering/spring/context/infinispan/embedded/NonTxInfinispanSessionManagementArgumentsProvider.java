/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.Set;

/**
 * @author Paul Ferraro
 */
public class NonTxInfinispanSessionManagementArgumentsProvider extends AbstractInfinispanSessionManagementArgumentsProvider {

	public NonTxInfinispanSessionManagementArgumentsProvider() {
		super(Set.of("dist", "repl"));
	}
}
