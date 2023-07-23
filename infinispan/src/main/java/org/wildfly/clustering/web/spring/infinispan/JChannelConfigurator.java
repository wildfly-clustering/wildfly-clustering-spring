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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.servlet.ServletContext;

import org.infinispan.commons.util.StringPropertyReplacer;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.remoting.transport.jgroups.JGroupsChannelConfigurator;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.ChannelListener;
import org.jgroups.EmptyMessage;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestCorrelator.Header;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.fork.UnknownForkHandler;
import org.jgroups.protocols.FORK;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.jgroups.util.DefaultThreadFactory;
import org.jgroups.util.SocketFactory;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class JChannelConfigurator implements JGroupsChannelConfigurator {

	static final ByteBuffer UNKNOWN_FORK_RESPONSE = ByteBuffer.allocate(0);

	private final String name;
	private final ProtocolStackConfigurator configurator;

	public JChannelConfigurator(ServletContext context, TransportConfiguration transport) throws IOException {
		this.name = transport.stack();
		this.configurator = getProtocolStackConfigurator(context, transport);
	}

	private static ProtocolStackConfigurator getProtocolStackConfigurator(ServletContext context, TransportConfiguration transport) throws IOException {
		Properties properties = transport.properties();
		if (properties.containsKey(JGroupsTransport.CHANNEL_CONFIGURATOR)) {
			return (JGroupsChannelConfigurator) properties.get(JGroupsTransport.CHANNEL_CONFIGURATOR);
		}
		if (properties.containsKey(JGroupsTransport.CONFIGURATION_XML)) {
			String xml = properties.getProperty(JGroupsTransport.CONFIGURATION_XML);
			try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
				return XmlConfigurator.getInstance(input);
			}
		}
		String resource = properties.getProperty(JGroupsTransport.CONFIGURATION_FILE, "default-configs/default-jgroups-udp.xml");
		URL url = context.getClassLoader().getResource(resource);
		if (url == null) {
			url = context.getResource(resource);
		}
		if (url == null) {
			throw new FileNotFoundException(resource);
		}
		try (InputStream input = url.openStream()) {
			return XmlConfigurator.getInstance(input);
		}
	}

	@Override
	public String getProtocolStackString() {
		return this.configurator.getProtocolStackString();
	}

	@Override
	public List<ProtocolConfiguration> getProtocolStack() {
		return this.configurator.getProtocolStack();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public JChannel createChannel(String name) throws Exception {
		FORK fork = new FORK();
		fork.setUnknownForkHandler(new UnknownForkHandler() {
			private final short id = ClassConfigurator.getProtocolId(RequestCorrelator.class);

			@Override
			public Object handleUnknownForkStack(Message message, String forkStackId) {
				return this.handle(message);
			}

			@Override
			public Object handleUnknownForkChannel(Message message, String forkChannelId) {
				return this.handle(message);
			}

			private Object handle(Message message) {
				Header header = (Header) message.getHeader(this.id);
				// If this is a request expecting a response, don't leave the requester hanging - send an identifiable response on which it can filter
				if ((header != null) && (header.type == Header.REQ) && header.rspExpected()) {
					Message response = new EmptyMessage(message.src()).setFlag(message.getFlags(), false).clearFlag(Message.Flag.RSVP);
					if (message.getDest() != null) {
						response.src(message.getDest());
					}

					response.putHeader(FORK.ID, message.getHeader(FORK.ID));
					response.putHeader(this.id, new Header(Header.RSP, header.req_id, header.corrId));

					fork.getProtocolStack().getChannel().down(response);
				}
				return null;			}
		});
		List<ProtocolConfiguration> configurations = this.getProtocolStack();
		List<Protocol> protocols = new ArrayList<>(configurations.size() + 1);
		Properties properties = WildFlySecurityManager.getSystemPropertiesPrivileged();
		for (ProtocolConfiguration configuration : configurations) {
			String protocolName = configuration.getProtocolName();
			String className = protocolName.startsWith(Global.PREFIX) ? protocolName : Global.PREFIX + protocolName;
			PrivilegedExceptionAction<Protocol> action = new PrivilegedExceptionAction<>() {
				@Override
				public Protocol run() throws Exception {
					try {
						Class<? extends Protocol> protocolClass = Protocol.class.getClassLoader().loadClass(className).asSubclass(Protocol.class);
						Protocol protocol = protocolClass.getConstructor().newInstance();
						StringPropertyReplacer.replaceProperties(configuration.getProperties(), properties);
						StackType type = Util.getIpStackType();
						Configurator.initializeAttrs(protocol, configuration, type);
						return protocol;
					} catch (InstantiationException | IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				}
			};
			protocols.add(WildFlySecurityManager.doUnchecked(action));
		}
		// Add implicit FORK to the top of the stack
		protocols.add(fork);
		TP transport = (TP) protocols.get(0);
		transport.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("jgroups", false, true), WildFlySecurityManager.getClassLoaderPrivileged(JChannelConfigurator.class)));

		return new JChannel(protocols);
	}

	@Override
	public void setSocketFactory(SocketFactory socketFactory) {
	}

	@Override
	public void addChannelListener(ChannelListener listener) {
	}
}
