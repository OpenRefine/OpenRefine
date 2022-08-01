/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.RefineServlet;

public class HttpClient {

    final static Logger logger = LoggerFactory.getLogger("http-client");

    final private RequestConfig defaultRequestConfig;
    private HttpClientBuilder httpClientBuilder;
    private CloseableHttpClient httpClient;
    private int _delay;
    private int _retryInterval; // delay between original request and first retry, in ms
    private HttpHost proxy;
    private int proxyPort;
    private String proxyHost;
    private DefaultProxyRoutePlanner routePlanner;

    public HttpClient() {
        this(0);
    }

    public HttpClient(int delay) {
        this(delay, Math.max(delay, 200));
    }

    public HttpClient(int delay, int retryInterval) {
        _delay = delay;
        _retryInterval = retryInterval;
        // Create a connection manager with a custom socket timeout
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        final SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(60, TimeUnit.SECONDS)
                .build();
        connManager.setDefaultSocketConfig(socketConfig);

        defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(60, TimeUnit.SECONDS)
                .setConnectionRequestTimeout(60, TimeUnit.SECONDS)
                .build();

        httpClientBuilder = HttpClients.custom()
                .setUserAgent(RefineServlet.getUserAgent())
                .setDefaultRequestConfig(defaultRequestConfig)
                .setConnectionManager(connManager)
                // Default Apache HC retry is 1x @1 sec (or the value in Retry-Header)
                .setRetryStrategy(new ExponentialBackoffRetryStrategy(3, TimeValue.ofMilliseconds(_retryInterval)))
//                .setRedirectStrategy(new LaxRedirectStrategy()) // TODO: No longer needed since default doesn't exclude POST?
//               .setConnectionBackoffStrategy(ConnectionBackoffStrategy)
                .addRequestInterceptorFirst(new HttpRequestInterceptor() {

                    private long nextRequestTime = System.currentTimeMillis();

                    @Override
                    public void process(
                            final HttpRequest request,
                            final EntityDetails entity,
                            final HttpContext context) throws HttpException, IOException {

                        long delay = nextRequestTime - System.currentTimeMillis();
                        if (delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException e) {
                            }
                        }
                        nextRequestTime = System.currentTimeMillis() + _delay;

                    }
                });

        if (System.getProperty("http.proxyHost") != null) {
            proxyHost = System.getProperty("http.proxyHost");
        }

        if (System.getProperty("http.proxyPort") != null) {
            proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
        }

        if (proxyHost != null && proxyPort != 0) {
            proxy = new HttpHost("http", proxyHost, proxyPort);
            httpClientBuilder.setProxy(proxy);
        }
        if (proxy != null) {
            routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpClientBuilder.setRoutePlanner(routePlanner);
        }

        // TODO: Placeholder for future Basic Auth implementation
//        String userinfo = url.getUserInfo();
//        // HTTPS only - no sending password in the clear over HTTP
//        if ("https".equals(url.getProtocol()) && userinfo != null) {
//            int s = userinfo.indexOf(':');
//            if (s > 0) {
//                String user = userinfo.substring(0, s);
//                String pw = userinfo.substring(s + 1, userinfo.length());
//                CredentialsProvider credsProvider = new BasicCredentialsProvider();
//                credsProvider.setCredentials(new AuthScope(url.getHost(), 443),
//                        new UsernamePasswordCredentials(user, pw.toCharArray()));
//                httpClientBuilder = httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
//            }
//        }

        httpClient = httpClientBuilder.build();
    }

    public String getAsString(String urlString, Header[] headers) throws IOException {

        final HttpClientResponseHandler<String> responseHandler = new HttpClientResponseHandler<String>() {

            @Override
            public String handleResponse(final ClassicHttpResponse response) throws IOException {
                final int status = response.getCode();
                if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
                    final HttpEntity entity = response.getEntity();
                    if (entity == null) {
                        throw new IOException("No content found in " + urlString);
                    }
                    try {
                        return EntityUtils.toString(entity);
                    } catch (final ParseException ex) {
                        throw new ClientProtocolException(ex);
                    }
                } else {
                    // String errorBody = EntityUtils.toString(response.getEntity());
                    throw new ClientProtocolException(String.format("HTTP error %d : %s for URL %s", status,
                            response.getReasonPhrase(), urlString));
                }
            }
        };

        return getResponse(urlString, headers, responseHandler);
    }

    public String getResponse(String urlString, Header[] headers, HttpClientResponseHandler<String> responseHandler) throws IOException {
        try {
            // Use of URL constructor below is purely to get additional error checking to mimic
            // previous behavior for the tests.
            new URL(urlString).toURI();
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
            return null;
        }

        HttpGet httpGet = new HttpGet(urlString);

        if (headers != null && headers.length > 0) {
            httpGet.setHeaders(headers);
        }
        httpGet.setConfig(defaultRequestConfig); // FIXME: Redundant? already includes in client builder
        return httpClient.execute(httpGet, responseHandler);
    }

    public String postNameValue(String serviceUrl, String name, String value) throws IOException {
        HttpPost request = new HttpPost(serviceUrl);
        List<NameValuePair> body = Collections.singletonList(
                new BasicNameValuePair(name, value));
        request.setEntity(new UrlEncodedFormEntity(body, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String reasonPhrase = response.getReasonPhrase();
            int statusCode = response.getCode();
            if (statusCode >= 400) { // We should never see 3xx since they get handled automatically
                throw new IOException(String.format("HTTP error %d : %s for URL %s", statusCode, reasonPhrase,
                        request.getRequestUri()));
            }

            return ParsingUtilities.inputStreamToString(response.getEntity().getContent());
        }
    }

    /**
     * Use binary exponential backoff strategy, instead of the default fixed retry interval, if the server doesn't
     * provide a Retry-After time.
     */
    class ExponentialBackoffRetryStrategy extends DefaultHttpRequestRetryStrategy {

        private final TimeValue defaultInterval;

        public ExponentialBackoffRetryStrategy(final int maxRetries, final TimeValue defaultRetryInterval) {
            super(maxRetries, defaultRetryInterval);
            this.defaultInterval = defaultRetryInterval;
        }

        @Override
        public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
            // Get the default implementation's interval
            TimeValue interval = super.getRetryInterval(response, execCount, context);
            // If it's the same as the default, there was no Retry-After, so use binary
            // exponential backoff
            if (interval.compareTo(defaultInterval) == 0) {
                interval = TimeValue.of(((Double) (Math.pow(2, execCount - 1) * defaultInterval.getDuration())).longValue(),
                        defaultInterval.getTimeUnit());
                logger.warn("Retrying HTTP request after " + interval.toString());
                return interval;
            }
            logger.warn("Retrying HTTP request after " + interval.toString());
            return interval;
        }

        /**
         * Even our POST requests should be retried, they are deemed idempotent
         */
        @Override
        public boolean handleAsIdempotent(final HttpRequest request) {
            return true;
        }
    }
}
