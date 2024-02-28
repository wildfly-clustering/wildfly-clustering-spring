/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.springframework.beans.factory.DisposableBean;

/**
 * Base class for beans that need to register some task to execute on destroy.
 * @author Paul Ferraro
 */
public class AutoDestroyBean implements DisposableBean, Consumer<Runnable> {
	private final Logger logger = Logger.getLogger(this.getClass());
	private final List<Runnable> tasks = new LinkedList<>();

	@Override
	public void accept(Runnable task) {
		// Use LIFO ordering.
		this.tasks.add(0, task);
	}

	@Override
	public void destroy() throws Exception {
		Iterator<Runnable> tasks = this.tasks.iterator();
		while (tasks.hasNext()) {
			try {
				tasks.next().run();
			} catch (RuntimeException | Error e) {
				this.logger.warn(e.getLocalizedMessage(), e);
			} finally {
				tasks.remove();
			}
		}
	}
}
