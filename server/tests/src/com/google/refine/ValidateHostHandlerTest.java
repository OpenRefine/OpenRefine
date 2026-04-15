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