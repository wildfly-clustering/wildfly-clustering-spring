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

package org.wildfly.clustering.web.spring.security.web.authentication.preauth;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.web.spring.security.web.authentication.HttpServletRequestBuilder;
import org.wildfly.clustering.web.spring.security.web.authentication.HttpServletRequestMarshaller;
import org.wildfly.clustering.web.spring.security.web.authentication.MockHttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class PreAuthenticatedWebAuthenticationDetailsMarshaller implements ProtoStreamMarshaller<PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> {

	private static final int HTTP_SERVLET_REQUEST_INDEX = 1;
	private static final int AUTHORITIY_INDEX = HTTP_SERVLET_REQUEST_INDEX + HttpServletRequestMarshaller.INSTANCE.getFields();

	@Override
	public PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails readFrom(ProtoStreamReader reader) throws IOException {
		HttpServletRequestBuilder builder = HttpServletRequestMarshaller.INSTANCE.getBuilder();
		List<GrantedAuthority> authorities = new LinkedList<>();
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			int index = WireType.getTagFieldNumber(tag);
			if (index >= HTTP_SERVLET_REQUEST_INDEX && index < AUTHORITIY_INDEX) {
				builder = HttpServletRequestMarshaller.INSTANCE.readField(reader, index - HTTP_SERVLET_REQUEST_INDEX, builder);
			} else if (index == AUTHORITIY_INDEX) {
				authorities.add(reader.readAny(GrantedAuthority.class));
			} else {
				reader.skipField(tag);
			}
		}
		return new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(builder.build(), authorities);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails details) throws IOException {
		HttpServletRequestMarshaller.INSTANCE.writeFields(writer, HTTP_SERVLET_REQUEST_INDEX, new MockHttpServletRequest(details));
		for (GrantedAuthority authority : details.getGrantedAuthorities()) {
			writer.writeAny(AUTHORITIY_INDEX, authority);
		}
	}

	@Override
	public Class<? extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> getJavaClass() {
		return PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails.class;
	}
}
