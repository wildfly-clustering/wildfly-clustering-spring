/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.web.context;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.wildfly.clustering.spring.web.SmokeITParameters;

/**
 * @author Paul Ferraro
 */
public class ReactiveConfig {

	@Bean
	public HandlerMapping handlerMapping() {
		return new SimpleUrlHandlerMapping(Map.of(SmokeITParameters.ENDPOINT_PATH, new SessionHandler()));
	}

	@Bean(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME)
	public WebHandler webHandler() {
		return new DispatcherHandler();
	}

	@Bean
	public HandlerAdapter handlerAdapter() {
		return new SimpleHandlerAdapter();
	}
}
