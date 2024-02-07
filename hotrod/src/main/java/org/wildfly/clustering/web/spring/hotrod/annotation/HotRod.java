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

package org.wildfly.clustering.web.spring.hotrod.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures the HotRod client used by a session repository. 
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.infinispan.remote.annotation.HotRod} instead.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface HotRod {
	/**
	 * The HotRod URI.
	 * @return a HotRod URI.
	 */
	String uri();

	/**
	 * The Infinispan server configuration template from which to create a remote cache.
	 * @return an Infinispan server template
	 */
	String template() default "org.infinispan.DIST_SYNC";

	/**
	 * The HotRod client properties.
	 * @return an array of HotRod client properties.
	 */
	Property[] properties() default {};
}
