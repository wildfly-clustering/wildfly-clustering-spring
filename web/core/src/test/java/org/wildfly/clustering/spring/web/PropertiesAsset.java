/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * A shrink-wrap asset for {@link Properties}.
 * @author Paul Ferraro
 */
public class PropertiesAsset implements Asset {

	private final Asset asset;

	public PropertiesAsset(Properties properties) {
		StringWriter writer = new StringWriter();
		try {
			properties.store(writer, null);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		this.asset = new StringAsset(writer.toString());
	}

	@Override
	public InputStream openStream() {
		return this.asset.openStream();
	}

	@Override
	public String toString() {
		return this.asset.toString();
	}
}
