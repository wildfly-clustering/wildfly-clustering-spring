/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.session.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.wildfly.clustering.spring.session.SpringSession;

/**
 * @author Paul Ferraro
 */
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	FindByIndexNameSessionRepository<SpringSession> repository;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {
		return http.httpBasic(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth.requestMatchers("/").hasRole("ADMIN").anyRequest().authenticated())
				.securityContext(context -> context.requireExplicitSave(false).securityContextRepository(new HttpSessionSecurityContextRepository()))
				.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(1).sessionRegistry(this.sessionRegistry()))
				.csrf(configurator -> configurator.disable())
				.build();
	}

	@SuppressWarnings("deprecation")
	@Bean
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
		manager.createUser(User.withDefaultPasswordEncoder().username("admin").password("password").roles("ADMIN").build());
		return manager;
	}

	@Bean
	public SpringSessionBackedSessionRegistry<SpringSession> sessionRegistry() {
		return new SpringSessionBackedSessionRegistry<>(this.repository);
	}
}
