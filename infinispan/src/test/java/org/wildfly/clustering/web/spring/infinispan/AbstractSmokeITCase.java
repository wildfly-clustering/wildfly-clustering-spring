/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.spring.infinispan;

import java.net.URL;
import java.util.function.BiFunction;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;

/**
 * @author Paul Ferraro
 */
public class AbstractSmokeITCase extends org.wildfly.clustering.web.spring.AbstractSmokeITCase {

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    private ContainerController controller;

    protected AbstractSmokeITCase() {
        super();
    }

    protected AbstractSmokeITCase(BiFunction<URL, URL, CloseableHttpClient> provider) {
        super(provider);
    }

    @Before
    public void init() {
        this.controller.start(CONTAINER_1);
        this.deployer.deploy(DEPLOYMENT_1);
        this.controller.start(CONTAINER_2);
        this.deployer.deploy(DEPLOYMENT_2);
    }

    @After
    public void destroy() {
        this.deployer.undeploy(DEPLOYMENT_2);
        this.controller.stop(CONTAINER_2);
        this.deployer.undeploy(DEPLOYMENT_1);
        this.controller.stop(CONTAINER_1);
    }
}
