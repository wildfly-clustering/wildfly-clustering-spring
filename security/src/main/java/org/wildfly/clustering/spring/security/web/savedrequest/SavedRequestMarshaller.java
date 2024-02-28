/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security.web.savedrequest;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;

import org.infinispan.protostream.descriptors.WireType;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedCookie;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.spring.security.Scheme;

/**
 * Marshaller for a {@link DefaultSavedRequest} that avoids marshalling of redundant info, e.g. request URI/URL, GET parameters, etc.
 * @author Paul Ferraro
 */
public class SavedRequestMarshaller implements ProtoStreamMarshaller<DefaultSavedRequest> {

	private static final int METHOD_INDEX = 1;
	private static final int SCHEME_INDEX = 2;
	private static final int SERVER_NAME_INDEX = 3;
	private static final int SERVER_PORT_INDEX = 4;
	private static final int CONTEXT_PATH_INDEX = 5;
	private static final int SERVLET_PATH_INDEX = 6;
	private static final int QUERY_INDEX = 7;
	private static final int PARAMETER_NAME_INDEX = 8;
	private static final int PARAMETER_VALUE_INDEX = 9;
	private static final int PARAMETER_VALUES_INDEX = 10;
	private static final int HEADER_NAME_INDEX = 11;
	private static final int HEADER_VALUE_INDEX = 12;
	private static final int HEADER_VALUES_INDEX = 13;
	private static final int LOCALE_INDEX = 14;
	private static final int COOKIE_INDEX = 15;
	// This is uncommon
	private static final int PATH_INFO_INDEX = 16;

	private static final String DEFAULT_METHOD = HttpMethod.GET.name();
	private static final Scheme DEFAULT_SCHEME = Scheme.HTTPS;
	private static final String DEFAULT_SERVER_NAME = InetAddress.getLoopbackAddress().getHostName();
	private static final int DEFAULT_SERVER_PORT = Scheme.HTTP.getDefaultPort();
	private static final String DEFAULT_CONTEXT_PATH = ""; // ROOT context
	private static final String DEFAULT_SERVLET_PATH = "";

	@Override
	public DefaultSavedRequest readFrom(ProtoStreamReader reader) throws IOException {
		DefaultSavedRequest.Builder builder = new DefaultSavedRequest.Builder();
		builder.setMethod(DEFAULT_METHOD);
		builder.setScheme(DEFAULT_SCHEME.getName());
		builder.setServerName(DEFAULT_SERVER_NAME);
		builder.setServerPort(DEFAULT_SERVER_PORT);
		builder.setContextPath(DEFAULT_CONTEXT_PATH);
		builder.setServletPath(DEFAULT_SERVLET_PATH);
		List<String> parameterNames = new LinkedList<>();
		List<String[]> parameterValues = new LinkedList<>();
		List<String> headerNames = new LinkedList<>();
		List<String[]> headerValues = new LinkedList<>();
		List<Locale> locales = new LinkedList<>();
		List<SavedCookie> cookies = new LinkedList<>();
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			switch (WireType.getTagFieldNumber(tag)) {
				case METHOD_INDEX:
					builder.setMethod(reader.readString());
					break;
				case SCHEME_INDEX:
					builder.setScheme(reader.readEnum(Scheme.class).getName());
					break;
				case SERVER_NAME_INDEX:
					builder.setServerName(reader.readString());
					break;
				case SERVER_PORT_INDEX:
					builder.setServerPort(reader.readUInt32());
					break;
				case CONTEXT_PATH_INDEX:
					builder.setContextPath(reader.readString());
					break;
				case SERVLET_PATH_INDEX:
					builder.setServletPath(reader.readString());
					break;
				case PATH_INFO_INDEX:
					builder.setPathInfo(reader.readString());
					break;
				case QUERY_INDEX:
					builder.setQueryString(reader.readString());
					break;
				case PARAMETER_NAME_INDEX:
					parameterNames.add(reader.readString());
					break;
				case PARAMETER_VALUE_INDEX:
					parameterValues.add(new String[] { reader.readString() });
					break;
				case PARAMETER_VALUES_INDEX:
					parameterValues.add(reader.readAny(String[].class));
					break;
				case HEADER_NAME_INDEX:
					headerNames.add(reader.readString());
					break;
				case HEADER_VALUE_INDEX:
					headerValues.add(new String[] { reader.readString() });
					break;
				case HEADER_VALUES_INDEX:
					headerValues.add(reader.readAny(String[].class));
					break;
				case LOCALE_INDEX:
					locales.add(reader.readObject(Locale.class));
					break;
				case COOKIE_INDEX:
					cookies.add(reader.readObject(SavedCookie.class));
					break;
				default:
					reader.skipField(tag);
			}
		}
		if (!parameterNames.isEmpty()) {
			Map<String, String[]> parameters = new TreeMap<>();
			Iterator<String> names = parameterNames.iterator();
			Iterator<String[]> values = parameterValues.iterator();
			while (names.hasNext() && values.hasNext()) {
				parameters.put(names.next(), values.next());
			}
			builder.setParameters(parameters);
		}
		if (!headerNames.isEmpty()) {
			Map<String, List<String>> headers = new TreeMap<>();
			Iterator<String> names = headerNames.iterator();
			Iterator<String[]> values = headerValues.iterator();
			while (names.hasNext() && values.hasNext()) {
				headers.put(names.next(), Arrays.asList(values.next()));
			}
			builder.setHeaders(headers);
		}
		builder.setLocales(locales);
		builder.setCookies(cookies);

		DefaultSavedRequest request = builder.build();
		String query = request.getQueryString();
		if (query != null) {
			// Parse query string into parameters
			Map<String, List<String>> map = new TreeMap<>();
			for (String parameter : query.split(Pattern.quote("&"))) {
				String[] values = parameter.split(Pattern.quote("="));
				String name = values[0];
				String value = values[1];
				List<String> list = map.get(name);
				if (list == null) {
					list = new LinkedList<>();
					map.put(name, list);
				}
				list.add(value);
			}
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				request.getParameterMap().put(entry.getKey(), entry.getValue().toArray(new String[0]));
			}
			builder.setParameters(request.getParameterMap());
		}
		// Set request URI
		StringBuilder uriBuilder = new StringBuilder();
		if (request.getContextPath() != null) {
			uriBuilder.append(request.getContextPath());
		}
		if (request.getServletPath() != null) {
			uriBuilder.append(request.getServletPath());
		}
		if (request.getPathInfo() != null) {
			uriBuilder.append(request.getPathInfo());
		}
		String requestURI = uriBuilder.toString();
		builder.setRequestURI(requestURI);

		// Set request URL
		StringBuilder urlBuilder = new StringBuilder(request.getScheme()).append("://").append(request.getServerName());
		if (Scheme.resolve(request.getScheme()).getDefaultPort() != request.getServerPort()) {
			urlBuilder.append(':').append(request.getServerPort());
		}
		builder.setRequestURL(urlBuilder.append(requestURI).toString());
		return builder.build();
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, DefaultSavedRequest request) throws IOException {
		// DefaultSavedRequest has a lot of redundant properties, so just persist the essentials
		String method = request.getMethod();
		if (!method.equals(DEFAULT_METHOD)) {
			writer.writeString(METHOD_INDEX, method);
		}
		Scheme scheme = Scheme.resolve(request.getScheme());
		if (scheme != DEFAULT_SCHEME) {
			writer.writeEnum(SCHEME_INDEX, scheme);
		}
		String serverName = request.getServerName();
		if (!serverName.equals(DEFAULT_SERVER_NAME)) {
			writer.writeString(SERVER_NAME_INDEX, serverName);
		}
		int serverPort = request.getServerPort();
		if (serverPort != DEFAULT_SERVER_PORT) {
			writer.writeUInt32(SERVER_PORT_INDEX, serverPort);
		}
		String contextPath = request.getContextPath();
		if (!contextPath.equals(DEFAULT_CONTEXT_PATH)) {
			writer.writeString(CONTEXT_PATH_INDEX, contextPath);
		}
		String servletPath = request.getServletPath();
		if (!servletPath.equals(DEFAULT_SERVLET_PATH)) {
			writer.writeString(SERVLET_PATH_INDEX, servletPath);
		}
		String pathInfo = request.getPathInfo();
		if (pathInfo != null) {
			writer.writeString(PATH_INFO_INDEX, pathInfo);
		}
		if (method.equals(HttpMethod.GET.name())) {
			// Because DefaultSavedRequest.doesRequestMatch(...) relies on the query string, we need to persist this to preserve order of parameters
			String query = request.getQueryString();
			if (query != null) {
				writer.writeString(QUERY_INDEX, query);
			}
		} else if (method.equals(HttpMethod.POST.name())) {
			// Only persist parameters for POST - otherwise parameters are already captured by query string
			for (String parameterName : request.getParameterNames()) {
				writer.writeString(PARAMETER_NAME_INDEX, parameterName);
				String[] parameterValues = request.getParameterValues(parameterName);
				if (parameterValues.length == 1) {
					writer.writeString(PARAMETER_VALUE_INDEX, parameterValues[0]);
				} else {
					writer.writeAny(PARAMETER_VALUES_INDEX, parameterValues);
				}
			}
		}
		for (String headerName : request.getHeaderNames()) {
			writer.writeString(HEADER_NAME_INDEX, headerName);
			List<String> headerValues = request.getHeaderValues(headerName);
			if (headerValues.size() == 1) {
				writer.writeString(HEADER_VALUE_INDEX, headerValues.get(0));
			} else {
				writer.writeAny(HEADER_VALUES_INDEX, headerValues.toArray(new String[0]));
			}
		}
		for (Locale locale : request.getLocales()) {
			writer.writeObject(LOCALE_INDEX, locale);
		}
		for (Cookie cookie : request.getCookies()) {
			writer.writeObject(COOKIE_INDEX, new SavedCookie(cookie));
		}
	}

	@Override
	public Class<? extends DefaultSavedRequest> getJavaClass() {
		return DefaultSavedRequest.class;
	}
}
