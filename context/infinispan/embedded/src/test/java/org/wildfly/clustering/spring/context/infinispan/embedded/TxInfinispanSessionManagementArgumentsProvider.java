/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.Set;

/**
 * @author Paul Ferraro
 */
public class TxInfinispanSessionManagementArgumentsProvider extends AbstractInfinispanSessionManagementArgumentsProvider {

	public TxInfinispanSessionManagementArgumentsProvider() {
		super(Set.of("dist-tx", "repl-tx"));
	}
}
