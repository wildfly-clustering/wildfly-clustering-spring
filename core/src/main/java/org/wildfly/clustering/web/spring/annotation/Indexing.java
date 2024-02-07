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

package org.wildfly.clustering.web.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.session.IndexResolver;
import org.springframework.session.PrincipalNameIndexResolver;
import org.wildfly.clustering.spring.session.config.SessionRepositoryConfigurationBean;

/**
 * Configures the indexing characteristics of an indexed session repository.
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.annotation.Indexing} instead.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Indexing {
	/**
	 * The indexes recognized by this session repository.  Default indexes only includes the Spring Security principal.
	 * @return an array of index names
	 */
	Index[] indexes() default { @Index(id = SessionRepositoryConfigurationBean.DEFAULT_SPRING_SECURITY_INDEX_ID, name = SessionRepositoryConfigurationBean.DEFAULT_STRING_SECURITY_INDEX_NAME) };

	/**
	 * The index resolver class name.  Default resolver only resolves the Spring Security principal.
	 * @return an index resolver class.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends IndexResolver> resolverClass() default PrincipalNameIndexResolver.class;
}
