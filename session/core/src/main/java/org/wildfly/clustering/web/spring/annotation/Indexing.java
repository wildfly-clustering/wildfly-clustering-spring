/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.session.IndexResolver;
import org.springframework.session.PrincipalNameIndexResolver;
import org.wildfly.clustering.spring.session.config.HttpSessionConfiguration;

/**
 * Configures the indexing characteristics of an indexed session repository.
 * @author Paul Ferraro
 * @deprecated Use {@link org.wildfly.clustering.spring.session.config.annotation.Indexing} instead.
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
	Index[] indexes() default { @Index(id = HttpSessionConfiguration.DEFAULT_SPRING_SECURITY_INDEX_ID, name = HttpSessionConfiguration.DEFAULT_STRING_SECURITY_INDEX_NAME) };

	/**
	 * The index resolver class name.  Default resolver only resolves the Spring Security principal.
	 * @return an index resolver class.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends IndexResolver> resolverClass() default PrincipalNameIndexResolver.class;
}
