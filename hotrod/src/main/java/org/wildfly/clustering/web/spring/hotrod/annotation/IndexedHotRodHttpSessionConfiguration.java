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

import java.net.URI;
import java.util.Properties;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.wildfly.clustering.web.spring.annotation.IndexedHttpSessionConfiguration;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepository;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepositoryConfiguration;

/**
 * Spring configuration bean for an indexed session repository whose sessions are persisted to a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class IndexedHotRodHttpSessionConfiguration extends IndexedHttpSessionConfiguration implements HotRodSessionRepositoryConfiguration {

    private URI uri;
    private Properties properties = new Properties();
    private String templateName = DefaultTemplate.DIST_SYNC.getTemplateName();

    public IndexedHotRodHttpSessionConfiguration() {
        super(EnableIndexedHotRodHttpSession.class);
    }

    @Bean
    public HotRodSessionRepository sessionRepository() {
        return new HotRodSessionRepository(this);
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public String getTemplateName() {
        return this.templateName;
    }

    @Autowired(required = false)
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Autowired(required = false)
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Autowired(required = false)
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Override
    public void accept(AnnotationAttributes attributes) {
        super.accept(attributes);
        AnnotationAttributes config = attributes.getAnnotation("config");
        this.uri = URI.create(config.getString("uri"));
        this.templateName = config.getString("template");
        for (AnnotationAttributes property : config.getAnnotationArray("properties")) {
            this.properties.setProperty(property.getString("name"), property.getString("value"));
        }
    }
}
