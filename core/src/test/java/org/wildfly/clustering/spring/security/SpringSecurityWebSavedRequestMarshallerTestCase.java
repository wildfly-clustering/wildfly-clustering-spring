/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.spring.security;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedCookie;
import org.wildfly.clustering.marshalling.Tester;

/**
 * @author Paul Ferraro
 */
public class SpringSecurityWebSavedRequestMarshallerTestCase {

	@Test
	public void test() throws IOException {
		Tester<DefaultSavedRequest> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

		Map<String, String[]> parameters = new TreeMap<>();
		parameters.put("foo", new String[] { "true" });
		parameters.put("bar", new String[] { "1", "2" });

		DefaultSavedRequest.Builder builder = new DefaultSavedRequest.Builder();
		builder.setMethod(HttpMethod.GET.name());
		builder.setScheme(Scheme.HTTPS.getName());
		builder.setServerName(InetAddress.getLoopbackAddress().getHostName());
		builder.setServerPort(Scheme.HTTPS.getDefaultPort());
		builder.setServletPath("");
		builder.setContextPath("");
		builder.setQueryString("foo=true&bar=1&bar=2");
		builder.setParameters(parameters);
		builder.setRequestURI("");
		builder.setRequestURL(String.format("https://%s", InetAddress.getLoopbackAddress().getHostName()));

		tester.test(builder.build(), SpringSecurityWebSavedRequestMarshallerTestCase::assertEquals);

		builder = new DefaultSavedRequest.Builder();
		builder.setMethod(HttpMethod.POST.name());
		builder.setScheme(Scheme.HTTP.getName());
		builder.setServerName(InetAddress.getLocalHost().getHostName());
		builder.setServerPort(8080);
		builder.setContextPath("/foo");
		builder.setServletPath("/bar");
		builder.setPathInfo("/extra/path");
		builder.setParameters(parameters);
		builder.setHeaders(Collections.singletonMap("Accept", Collections.singletonList("text/html")));
		builder.setLocales(Collections.singletonList(Locale.US));
		Cookie cookie = new Cookie("name", "value");
		cookie.setDomain("domain");
		cookie.setMaxAge(100);
		cookie.setPath("/path");
		cookie.setSecure(true);
		builder.setCookies(Arrays.asList(new SavedCookie(new Cookie("foo", "bar")), new SavedCookie(cookie)));
		builder.setRequestURI("/foo/bar/extra/path");
		builder.setRequestURL(String.format("http://%s:8080/foo/bar/extra/path", InetAddress.getLocalHost().getHostName()));

		tester.test(builder.build(), SpringSecurityWebSavedRequestMarshallerTestCase::assertEquals);
	}

	private static void assertEquals(DefaultSavedRequest request1, DefaultSavedRequest request2) {
		Assertions.assertEquals(request1.getMethod(), request2.getMethod());
		Assertions.assertEquals(request1.getScheme(), request2.getScheme());
		Assertions.assertEquals(request1.getServerName(), request2.getServerName());
		Assertions.assertEquals(request1.getServerPort(), request2.getServerPort());
		Assertions.assertEquals(request1.getServletPath(), request2.getServletPath());
		Assertions.assertEquals(request1.getContextPath(), request2.getContextPath());
		Assertions.assertEquals(request1.getPathInfo(), request2.getPathInfo());
		Assertions.assertEquals(request1.getQueryString(), request2.getQueryString());
		Assertions.assertEquals(request1.getRequestURI(), request2.getRequestURI());
		Assertions.assertEquals(request1.getRequestURL(), request2.getRequestURL());
		Assertions.assertEquals(request1.getRedirectUrl(), request2.getRedirectUrl());
		Assertions.assertEquals(request1.getParameterNames(), request2.getParameterNames());
		for (String parameterName : request1.getParameterNames()) {
			Assertions.assertArrayEquals(request1.getParameterValues(parameterName), request2.getParameterValues(parameterName));
		}
		Assertions.assertEquals(request1.getHeaderNames(), request2.getHeaderNames());
		for (String headerName : request1.getHeaderNames()) {
			Assertions.assertEquals(request1.getHeaderValues(headerName), request2.getHeaderValues(headerName));
		}
		Assertions.assertEquals(request1.getLocales(), request2.getLocales());

		List<Cookie> cookies1 = request1.getCookies();
		List<Cookie> cookies2 = request2.getCookies();
		Assertions.assertEquals(cookies1.size(), cookies2.size());
		for (int i = 0; i < cookies1.size(); ++i) {
			Cookie cookie1 = cookies1.get(i);
			Cookie cookie2 = cookies2.get(i);

			Assertions.assertEquals(cookie1.getName(), cookie2.getName());
			Assertions.assertEquals(cookie1.getValue(), cookie2.getValue());
			Assertions.assertEquals(cookie1.getDomain(), cookie2.getDomain());
			Assertions.assertEquals(cookie1.getMaxAge(), cookie2.getMaxAge());
			Assertions.assertEquals(cookie1.getPath(), cookie2.getPath());
			Assertions.assertEquals(cookie1.getSecure(), cookie2.getSecure());
		}
	}
}
