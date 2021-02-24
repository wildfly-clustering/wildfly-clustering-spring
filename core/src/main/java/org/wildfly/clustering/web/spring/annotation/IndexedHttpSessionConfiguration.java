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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.IndexResolver;
import org.springframework.session.PrincipalNameIndexResolver;
import org.springframework.session.Session;

/**
 * Spring configuration bean for an indexed distributable session repository.
 * @author Paul Ferraro
 */
public abstract class IndexedHttpSessionConfiguration extends HttpSessionConfiguration {

    private Set<String> indexes = Collections.singleton(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
    private IndexResolver<Session> resolver = new PrincipalNameIndexResolver<>();

    protected IndexedHttpSessionConfiguration(Class<? extends Annotation> annotationClass) {
        super(annotationClass);
    }

    @Override
    public Set<String> getIndexes() {
        return this.indexes;
    }

    @Override
    public IndexResolver<Session> getIndexResolver() {
        return this.resolver;
    }

    @Autowired(required = false)
    public void setIndexes(String[] indexes) {
        this.indexes = new TreeSet<>(Arrays.asList(indexes));
    }

    @Autowired(required = false)
    public void setIndexResolver(IndexResolver<Session> resolver) {
        this.resolver = resolver;
    }

    @Autowired(required = false)
    public void setIndexResolverClass(@SuppressWarnings("rawtypes") Class<? extends IndexResolver> resolverClass) {
        try {
            this.resolver = resolverClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(resolverClass.getCanonicalName());
        }
    }

    @Override
    public void accept(AnnotationAttributes attributes) {
        super.accept(attributes);
        AnnotationAttributes indexing = attributes.getAnnotation("indexing");
        this.setIndexes(indexing.getStringArray("indexes"));
        this.setIndexResolverClass(indexing.getClass("resolverClass"));
    }
}
