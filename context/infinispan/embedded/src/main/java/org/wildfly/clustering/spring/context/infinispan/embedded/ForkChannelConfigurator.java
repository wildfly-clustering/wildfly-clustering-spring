/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.context.infinispan.embedded;

import java.util.List;

import javax.sql.DataSource;

import org.infinispan.remoting.transport.jgroups.JGroupsChannelConfigurator;
import org.jgroups.JChannel;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.fork.ForkChannel;
import org.jgroups.util.SocketFactory;

/**
 * @author Paul Ferraro
 */
public class ForkChannelConfigurator implements JGroupsChannelConfigurator {

	private final JChannel channel;
	private final String forkName;

	public ForkChannelConfigurator(JChannel channel, String forkName) {
		this.channel = channel;
		this.forkName = forkName;
	}

	@Override
	public String getProtocolStackString() {
		return null;
	}

	@Override
	public List<ProtocolConfiguration> getProtocolStack() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public JChannel createChannel(String name) throws Exception {
		// Silence log messages when Infinispan calls ForkChannel.setName(...)
		return new ForkChannel(this.channel, this.channel.getClusterName(), this.forkName) {
			@Override
			public ForkChannel setName(String name) {
				return this;
			}

			@Override
			public JChannel name(String name) {
				return this;
			}
		};
	}

	@Override
	public void setSocketFactory(SocketFactory socketFactory) {
	}

	@Override
	public void setDataSource(DataSource dataSource) {
	}
}
