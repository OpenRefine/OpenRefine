/*******************************************************************************
 * Copyright (C) 2020, OpenRefine contributors
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

package com.google.refine;

import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.Request;

/**
 * Validate the Host header of the HTTP request to see if it matches either a loopback IP address, localhost or an
 * explicitly specified hostname. This is required to avoid DNS rebinding attacks against users running OpenRefine on
 * their desktop computers.
 */
class ValidateHostHandler extends HandlerWrapper {

    /**
     * Matches: - addresses in the 127.0.0.0/8 subnet - IPv4-mapped addresses in the ::ffff:7f00:00/104 subnet -
     * different representations of ::1 - localhost Matching is a little fuzzy to simplify the regular expression - it
     * expects the Host header to be well-formed. Some invalid addresses would be accepted, for example: - 127.6..64.245
     * - 0::0:::0:00:1 This is not a problem however, as these are not valid DNS names either, and should never be sent
     * by a well-behaved browser - and validating the host header only ever helps if the browser works as expected and
     * cannot be used to fake the Host header.
     */
    static private final Pattern LOOPBACK_PATTERN = Pattern
            .compile("^(?:127\\.[0-9\\.]*|\\[[0\\:]*\\:(?:ffff\\:7f[0-9a-f]{2}:[0-9a-f]{1,4}|0{0,3}1)\\]|localhost)(?:\\:[0-9]+)?$",
                    Pattern.CASE_INSENSITIVE);

    private String expectedHost;

    public ValidateHostHandler(String expectedHost) {
        this.expectedHost = expectedHost;
    }

    public boolean isValidHost(String host) {

        // Allow loopback IPv4 and IPv6 addresses, as well as localhost
        if (LOOPBACK_PATTERN.matcher(host).find()) {
            return true;
        }

        // Strip port from hostname - for IPv6 addresses, if
        // they end with a bracket, then there is no port
        int index = host.lastIndexOf(':');
        if (index > 0 && !host.endsWith("]")) {
            host = host.substring(0, index);
        }

        // Strip brackets from IPv6 addresses
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 2);
        }

        // Allow only if stripped hostname matches expected hostname
        return expectedHost.equalsIgnoreCase(host);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String host = request.getHeader("Host");
        if (isValidHost(host)) {
            super.handle(target, baseRequest, request, response);
        } else {
            // Return HTTP 404 Not Found, since we are
            // not serving content for the requested URL
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid hostname");
        }
    }

}
