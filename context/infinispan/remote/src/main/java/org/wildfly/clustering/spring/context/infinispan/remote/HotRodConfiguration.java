/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.spring.context.infinispan.remote;

import java.net.URI;
import java.util.Properties;

/**
 * @author Paul Ferraro
 */
public interface HotRodConfiguration {
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

	URI getUri();
	Properties getProperties();
	String getTemplateName();
	String getConfiguration();
}
