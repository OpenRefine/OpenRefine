/*

Copyright 2026, OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ValidateHostHandlerTest {

    @Test
    public void testValidBracketedIpv6HostIsAccepted() {
        ValidateHostHandler handler = new ValidateHostHandler("2001:db8::1");

        Assert.assertTrue(handler.isValidHost("[2001:db8::1]:3333"));
    }

    @Test
    public void testNullOrEmptyHostIsRejected() {
        ValidateHostHandler handler = new ValidateHostHandler("127.0.0.1");

        Assert.assertFalse(handler.isValidHost(null));
        Assert.assertFalse(handler.isValidHost(""));
    }

    @Test
    public void testHandleRejectsMissingHostHeader() throws Exception {
        ValidateHostHandler handler = new ValidateHostHandler("127.0.0.1");
        Request baseRequest = mock(Request.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getHeader("Host")).thenReturn(null);

        handler.handle("/", baseRequest, request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid hostname");
    }
}
