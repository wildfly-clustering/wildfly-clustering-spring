/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.infinispan.embedded;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.clustering.spring.session.infinispan.embedded.context.Config;

/**
 * @author Paul Ferraro
 */
public class AnnotationInfinispanSessionManagerITCase extends AbstractInfinispanSessionManagerITCase {

	@Test
	public void test() {
		WebArchive archive = this.get()	.addPackage(Config.class.getPackage());
		this.accept(archive);
	}
}
