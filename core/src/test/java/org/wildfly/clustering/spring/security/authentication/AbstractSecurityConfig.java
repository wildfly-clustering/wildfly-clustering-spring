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

package org.wildfly.clustering.spring.security.authentication;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
public abstract class AbstractSecurityConfig implements Supplier<FindByIndexNameSessionRepository<SpringSession>> {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
		return new SpringSessionBackedSessionRegistry<>(this.get());
	}
}