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

package org.wildfly.clustering.web.spring.infinispan;

import java.util.List;

import org.infinispan.remoting.transport.jgroups.JGroupsChannelConfigurator;
import org.jgroups.ChannelListener;
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
		return new ForkChannel(this.channel, this.channel.getClusterName(), this.forkName);
	}

	@Override
	public void setSocketFactory(SocketFactory socketFactory) {
	}

	@Override
	public void addChannelListener(ChannelListener listener) {
		this.channel.addChannelListener(listener);
	}
}
