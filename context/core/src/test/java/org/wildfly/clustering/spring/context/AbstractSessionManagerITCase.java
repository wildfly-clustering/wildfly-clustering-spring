/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.nio.file.Path;
import java.util.function.Function;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.wildfly.clustering.junit.Tester;
import org.wildfly.clustering.session.container.SessionManagementTesterConfiguration;

/**
 * @author Paul Ferraro
 * @param <C> the argument type
 */
public abstract class AbstractSessionManagerITCase<C extends SessionManagementArguments> extends org.wildfly.clustering.session.container.AbstractSessionManagerITCase<C, WebArchive> {
	private static final Path POM_PATH = Path.of(System.getProperty("project.basedir"), "pom.xml");
	private static final Path ARTIFACT_PATH = Path.of(System.getProperty("project.artifact"));

	protected AbstractSessionManagerITCase(SessionManagementTesterConfiguration configuration) {
		super(configuration, WebArchive.class);
	}

	public AbstractSessionManagerITCase(Function<SessionManagementTesterConfiguration, Tester> testerFactory, SessionManagementTesterConfiguration configuration) {
		super(testerFactory, configuration, WebArchive.class);
	}

	@Override
	public WebArchive createArchive(C configuration) {
		return super.createArchive(configuration).addAsLibrary(ARTIFACT_PATH.toFile()).addAsLibraries(Maven.resolver().loadPomFromFile(POM_PATH.toFile()).importCompileAndRuntimeDependencies().resolve().withTransitivity().asFile());
	}
}
