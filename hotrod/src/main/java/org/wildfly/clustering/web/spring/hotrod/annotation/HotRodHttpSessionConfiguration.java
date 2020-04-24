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

package org.wildfly.clustering.web.spring.hotrod.annotation;

import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.web.context.ServletContextAware;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepository;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepositoryConfiguration;
import org.wildfly.common.Assert;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class HotRodHttpSessionConfiguration extends SpringHttpSessionConfiguration implements HotRodSessionRepositoryConfiguration, ServletContextAware, ApplicationEventPublisherAware, BeanClassLoaderAware {

    private Properties properties;
    private Integer maxActiveSessions;
    private SessionAttributePersistenceStrategy persistenceStrategy;
    private String configurationName;
    private Supplier<String> identifierFactory = () -> UUID.randomUUID().toString();
    private ApplicationEventPublisher publisher;
    private ServletContext context;
    private ClassLoader loader;

    @Bean
    public HotRodSessionRepository sessionRepository() {
        Assert.assertNotNull(this.properties);
        Assert.assertNotNull(this.persistenceStrategy);
        Assert.assertNotNull(this.publisher);
        Assert.assertNotNull(this.context);
        Assert.assertNotNull(this.loader);
        return new HotRodSessionRepository(this);
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public String getConfigurationName() {
        return this.configurationName;
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    @Override
    public SessionAttributePersistenceStrategy getPersistenceStrategy() {
        return this.persistenceStrategy;
    }

    @Override
    public Supplier<String> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public ApplicationEventPublisher getEventPublisher() {
        return this.publisher;
    }

    @Override
    public ServletContext getServletContext() {
        return this.context;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.loader;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void setServletContext(ServletContext context) {
        super.setServletContext(context);
        this.context = context;
    }

    @Override
    public void setBeanClassLoader(ClassLoader loader) {
        this.loader = loader;
    }

    @Autowired(required = true)
    @Qualifier("hotRodProperties")
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Autowired(required = true)
    public void setPersistenceStrategy(SessionAttributePersistenceStrategy persistenceStrategy) {
        this.persistenceStrategy = persistenceStrategy;
    }

    @Autowired(required = false)
    public void setMaxActiveSessions(Integer maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }

    @Autowired(required = false)
    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    @Autowired(required = false)
    public void setIdentifierFactory(Supplier<String> identifierFactory) {
        this.identifierFactory = identifierFactory;
    }
}
