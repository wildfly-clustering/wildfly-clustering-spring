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

package org.wildfly.clustering.web.spring.hotrod;

import java.net.URL;
import java.util.function.BiFunction;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.core.TestSystemPropertyNames;
import org.infinispan.server.test.junit4.InfinispanServerRuleBuilder;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSmokeITCase extends org.wildfly.clustering.web.spring.AbstractSmokeITCase {

    private static final String INFINISPAN_SERVER_HOME = System.getProperty("infinispan.server.home");
    private static final String INFINISPAN_DRIVER_USERNAME = "testsuite-driver-user";
    private static final String INFINISPAN_DRIVER_PASSWORD = "testsuite-driver-password";

    @ClassRule
    public static final TestRule SERVERS = InfinispanServerRuleBuilder.config(INFINISPAN_SERVER_HOME + "/server/conf/infinispan.xml")
            .property(TestSystemPropertyNames.INFINISPAN_TEST_SERVER_DIR, INFINISPAN_SERVER_HOME)
            .property("infinispan.client.rest.auth_username", INFINISPAN_DRIVER_USERNAME)
            .property("infinispan.client.rest.auth_password", INFINISPAN_DRIVER_PASSWORD)
            .runMode(ServerRunMode.FORKED)
            .numServers(1)
            .build();

    AbstractSmokeITCase() {
        super(false);
    }

    AbstractSmokeITCase(BiFunction<URL, URL, CloseableHttpClient> provider) {
        super(false, provider);
    }
}
