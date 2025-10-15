/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.net.URI;
import java.util.Properties;

/**
 * A HotRod client configuration.
 * @author Paul Ferraro
 */
public interface HotRodConfiguration {
	/** The default cache configuration */
	String DEFAULT_CONFIGURATION = """
{
	"distributed-cache" : {
		"mode" : "SYNC",
		"statistics" : "true",
		"encoding" : {
			"key" : {
				"media-type" : "application/octet-stream"
			},
			"value" : {
				"media-type" : "application/octet-stream"
			}
		},
		"transaction" : {
			"mode" : "NON_XA",
			"locking" : "PESSIMISTIC"
		}
	}
}""";

	/**
	 * Returns the HotRod client URI.
	 * @return the HotRod client URI.
	 */
	URI getUri();

	/**
	 * Returns the configuration properties of the HotRod client.
	 * @return the configuration properties of the HotRod client.
	 */
	Properties getProperties();

	/**
	 * Returns the name of a cache template.
	 * @return the name of a cache template.
	 */
	String getTemplate();

	/**
	 * Returns the cache configuration.
	 * @return the cache configuration.
	 */
	String getConfiguration();
}
