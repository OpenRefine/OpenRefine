/*

Copyright 2010, Google Inc.
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;

public class RefineServletTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System under test
    RefineServletStub SUT = null;

    // variables
    final static private String TEST_COMMAND_NAME = "test-command";
    final static private String TEST_COMMAND_PATH = "/command/core/test-command/foobar";
    final static private String BAD_COMMAND_PATH = "/command/core/command-does-not-exist";

    final static private String POST = "POST";
    final static private String GET = "GET";
    final static private String PUT = "PUT";
    final static private String DELETE = "DELETE";

    // mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;

    @BeforeMethod
    public void SetUp() throws ServletException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = mock(Command.class);

        SUT = new RefineServletStub();
        SUT.insertCommand(TEST_COMMAND_NAME, command); // inject mock into command container
    }

    @AfterMethod
    public void TearDown() {
        SUT.removeCommand(TEST_COMMAND_NAME); // remove mock to clean command container
        SUT = null;

        request = null;
        response = null;
        command = null;
    }

    // -------------------AutoSaveTimerTask tests-----------
    // TODO would need to mock Timer and inject it into RefineServlet. Also need to deal with ProjectManager.singleton
    // -------------------init tests------------------------
    // TODO need to stub super.init(), mock Timer and inject it into RefineServlet
    // -------------------destroy tests---------------------
    // TODO need to mock Timer and inject it into RefineServlet. Also need to deal with ProjectManager.singleton

    // --------------------doGet tests----------------------
    @Test
    public void doGetRegressionTest() {
        whenGetCommandNameThenReturn(TEST_COMMAND_PATH);
        whenGetMethodThenReturn(GET);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        try {
            verify(command, times(1)).doGet(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void doGetReturnsError404WhenCommandNotFound() {
        whenGetCommandNameThenReturn(BAD_COMMAND_PATH);
        whenGetMethodThenReturn(GET);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        verifyError404Called();

    }

    // ----------------doPost tests-------------------------
    @Test
    public void doPostRegressionTest() {
        whenGetCommandNameThenReturn(TEST_COMMAND_PATH);
        whenGetMethodThenReturn(POST);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        try {
            verify(command, times(1)).doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void doPostReturns404WhenCommandNotFound() {
        whenGetCommandNameThenReturn(BAD_COMMAND_PATH);
        whenGetMethodThenReturn(POST);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        verifyError404Called();
    }

    // ----------------doPut tests-------------------------
    @Test
    public void doPutRegressionTest() {
        whenGetCommandNameThenReturn(TEST_COMMAND_PATH);
        whenGetMethodThenReturn(PUT);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        try {
            verify(command, times(1)).doPut(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void doPutReturns404WhenCommandNotFound() {
        whenGetCommandNameThenReturn(BAD_COMMAND_PATH);
        whenGetMethodThenReturn(PUT);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        verifyError404Called();
    }

    // ----------------doDelete tests-------------------------
    @Test
    public void doDeleteRegressionTest() {
        whenGetCommandNameThenReturn(TEST_COMMAND_PATH);
        whenGetMethodThenReturn(DELETE);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        try {
            verify(command, times(1)).doDelete(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void doDeleteReturns404WhenCommandNotFound() {
        whenGetCommandNameThenReturn(BAD_COMMAND_PATH);
        whenGetMethodThenReturn(DELETE);

        try {
            SUT.wrapService(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled(2);
        verifyError404Called();
    }

    // ----------------getCommandName tests----------------

    @Test
    public void getCommandNameHandlesBadCommandName() {

        when(request.getPathInfo()).thenReturn("/command/this-command-has-no-trailing-slash");

        Assert.assertEquals("this-command-has-no-trailing-slash", SUT.wrapGetCommandName(request));

        verify(request, times(1)).getPathInfo();
    }

    // ------------helpers
    protected void whenGetCommandNameThenReturn(String commandName) {
        when(request.getPathInfo()).thenReturn(commandName);
    }

    protected void whenGetMethodThenReturn(String method) {
        when(request.getMethod()).thenReturn(method);
    }

    protected void verifyGetCommandNameCalled(int times) {
        verify(request, times(times)).getPathInfo();
    }

    protected void verifyError404Called() {
        try {
            verify(response, times(1)).sendError(HttpStatus.SC_NOT_FOUND);
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
