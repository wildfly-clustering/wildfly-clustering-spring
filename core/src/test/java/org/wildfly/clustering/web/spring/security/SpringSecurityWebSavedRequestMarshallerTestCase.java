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

package org.wildfly.clustering.web.spring.security;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.Cookie;

import org.junit.Assert;
import org.junit.Test;
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
        builder.setCookies(Arrays.asList(new SavedCookie("foo", "bar", null, null, -1, null, false, 0), new SavedCookie("name", "value", "comment", "domain", 100, "/path", true, 1)));
        builder.setRequestURI("/foo/bar/extra/path");
        builder.setRequestURL(String.format("http://%s:8080/foo/bar/extra/path", InetAddress.getLocalHost().getHostName()));

        tester.test(builder.build(), SpringSecurityWebSavedRequestMarshallerTestCase::assertEquals);
    }

    private static void assertEquals(DefaultSavedRequest request1, DefaultSavedRequest request2) {
        Assert.assertEquals(request1.getMethod(), request2.getMethod());
        Assert.assertEquals(request1.getScheme(), request2.getScheme());
        Assert.assertEquals(request1.getServerName(), request2.getServerName());
        Assert.assertEquals(request1.getServerPort(), request2.getServerPort());
        Assert.assertEquals(request1.getServletPath(), request2.getServletPath());
        Assert.assertEquals(request1.getContextPath(), request2.getContextPath());
        Assert.assertEquals(request1.getPathInfo(), request2.getPathInfo());
        Assert.assertEquals(request1.getQueryString(), request2.getQueryString());
        Assert.assertEquals(request1.getRequestURI(), request2.getRequestURI());
        Assert.assertEquals(request1.getRequestURL(), request2.getRequestURL());
        Assert.assertEquals(request1.getRedirectUrl(), request2.getRedirectUrl());
        Assert.assertEquals(request1.getParameterNames(), request2.getParameterNames());
        for (String parameterName : request1.getParameterNames()) {
            Assert.assertArrayEquals(request1.getParameterValues(parameterName), request2.getParameterValues(parameterName));
        }
        Assert.assertEquals(request1.getHeaderNames(), request2.getHeaderNames());
        for (String headerName : request1.getHeaderNames()) {
            Assert.assertEquals(request1.getHeaderValues(headerName), request2.getHeaderValues(headerName));
        }
        Assert.assertEquals(request1.getLocales(), request2.getLocales());

        List<Cookie> cookies1 = request1.getCookies();
        List<Cookie> cookies2 = request2.getCookies();
        Assert.assertEquals(cookies1.size(), cookies2.size());
        for (int i = 0; i < cookies1.size(); ++i) {
            Cookie cookie1 = cookies1.get(i);
            Cookie cookie2 = cookies2.get(i);

            Assert.assertEquals(cookie1.getName(), cookie2.getName());
            Assert.assertEquals(cookie1.getValue(), cookie2.getValue());
            Assert.assertEquals(cookie1.getComment(), cookie2.getComment());
            Assert.assertEquals(cookie1.getDomain(), cookie2.getDomain());
            Assert.assertEquals(cookie1.getMaxAge(), cookie2.getMaxAge());
            Assert.assertEquals(cookie1.getPath(), cookie2.getPath());
            Assert.assertEquals(cookie1.getSecure(), cookie2.getSecure());
            Assert.assertEquals(cookie1.getVersion(), cookie2.getVersion());
        }
    }
}
