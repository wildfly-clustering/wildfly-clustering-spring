/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.servlet.context;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Empty initializer that explicitly prevents dynamic registration of a servlet context listener,
 * as this must be declared explicitly (either via web.xml or {@link javax.servlet.annotation.WebListener} annotation).
 * @author Paul Ferraro
 */
public class HttpSessionApplicationInitializer extends AbstractHttpSessionApplicationInitializer {
}
