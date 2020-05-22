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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.clustering.web.spring.AbstractSmokeITCase;
import org.wildfly.clustering.web.spring.hotrod.servlet.SessionServlet;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SmokeITCase extends AbstractSmokeITCase {
    public static final String CONTAINER_1 = "tomcat-1";
    public static final String CONTAINER_2 = "tomcat-2";
    public static final String DEPLOYMENT_1 = "deployment-1";
    public static final String DEPLOYMENT_2 = "deployment-2";

    @Deployment(name = DEPLOYMENT_1, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment1() {
        return deployment(SmokeITCase.class, SessionServlet.class);
    }

    @Deployment(name = DEPLOYMENT_2, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment2() {
        return deployment(SmokeITCase.class, SessionServlet.class);
    }

    @Override
    @Test
    public void test(@ArquillianResource(SessionServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(SessionServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws IOException, URISyntaxException {
        super.test(baseURL1, baseURL2);
    }
}
