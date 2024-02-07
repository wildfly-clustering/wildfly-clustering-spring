/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.authentication.preauth;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.spring.security.web.authentication.HttpServletRequestBuilder;
import org.wildfly.clustering.spring.security.web.authentication.HttpServletRequestMarshaller;
import org.wildfly.clustering.spring.security.web.authentication.MockHttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class PreAuthenticatedWebAuthenticationDetailsMarshaller implements ProtoStreamMarshaller<PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> {

	private static final int HTTP_SERVLET_REQUEST_INDEX = 1;
	private static final int AUTHORITIY_INDEX = HTTP_SERVLET_REQUEST_INDEX + HttpServletRequestMarshaller.INSTANCE.getFields();

	@Override
	public PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails readFrom(ProtoStreamReader reader) throws IOException {
		FieldSetReader<HttpServletRequestBuilder> requestReader = reader.createFieldSetReader(HttpServletRequestMarshaller.INSTANCE, HTTP_SERVLET_REQUEST_INDEX);
		HttpServletRequestBuilder builder = HttpServletRequestMarshaller.INSTANCE.createInitialValue();
		List<GrantedAuthority> authorities = new LinkedList<>();
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			int index = WireType.getTagFieldNumber(tag);
			if (requestReader.contains(index)) {
				builder = requestReader.readField(builder);
			} else if (index == AUTHORITIY_INDEX) {
				authorities.add(reader.readAny(GrantedAuthority.class));
			} else {
				reader.skipField(tag);
			}
		}
		return new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(builder.get(), authorities);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails details) throws IOException {
		writer.createFieldSetWriter(HttpServletRequestMarshaller.INSTANCE, HTTP_SERVLET_REQUEST_INDEX).writeFields(new MockHttpServletRequest(details));
		for (GrantedAuthority authority : details.getGrantedAuthorities()) {
			writer.writeAny(AUTHORITIY_INDEX, authority);
		}
	}

	@Override
	public Class<? extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> getJavaClass() {
		return PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails.class;
	}
}
