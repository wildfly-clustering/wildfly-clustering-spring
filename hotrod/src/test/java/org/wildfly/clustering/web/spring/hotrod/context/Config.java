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

package org.wildfly.clustering.web.spring.hotrod.context;

import org.wildfly.clustering.web.spring.SessionMarshallerFactory;
import org.wildfly.clustering.web.spring.SessionPersistenceGranularity;
import org.wildfly.clustering.web.spring.annotation.SessionManager;
import org.wildfly.clustering.web.spring.hotrod.annotation.EnableHotRodHttpSession;
import org.wildfly.clustering.web.spring.hotrod.annotation.HotRod;

/**
 * Test configuration for session manager.
 * @author Paul Ferraro
 */
@EnableHotRodHttpSession(config = @HotRod(uri = "hotrod://127.0.0.1:11222", template = "default"), manager = @SessionManager(marshallerFactory = SessionMarshallerFactory.PROTOSTREAM, granularity = SessionPersistenceGranularity.ATTRIBUTE))
public class Config {

}
