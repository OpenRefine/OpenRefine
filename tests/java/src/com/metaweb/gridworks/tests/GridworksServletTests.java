package com.metaweb.gridworks.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.commands.Command;

public class GridworksServletTests {
    // logging
    final static protected Logger logger = LoggerFactory.getLogger("GridworksServletTests");

    //System under test
    GridworksServletStub SUT = null;

    //variables
    String TEST_COMMAND_NAME = "test-command";
    String TEST_COMMAND_PATH = "/test-command/foobar";
    String BAD_COMMAND_PATH = "/command-does-not-exist";

    //mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;


    @Before
    public void SetUp()
    {
        SUT = new GridworksServletStub();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = mock(Command.class);

        GridworksServletStub.InsertCommand(TEST_COMMAND_NAME,command); //inject mock into command container
    }

    @After
    public void TearDown(){
        SUT = null;
        request = null;
        response = null;
        command = null;
        GridworksServletStub.RemoveCommand(TEST_COMMAND_NAME); //remove mock to clean command container
    }
    //-------------------AutoSaveTimerTask tests-----------
    //TODO would need to mock Timer and inject it into GridworksServlet.  Also need to deal with ProjectManager.singleton
    //-------------------init tests------------------------
    //TODO need to stub super.init(), mock Timer and inject it into GridworksServlet
    //-------------------destroy tests---------------------
    //TODO need to mock Timer and inject it into GridworksServlet.  Also need to deal with ProjectManager.singleton

    //--------------------doGet tests----------------------
    @Test
    public void doGetRegressionTest(){
        whenGetCommandNameThenReturn(TEST_COMMAND_PATH);

        try {
            SUT.wrapDoGet(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled();
        try {
            verify(command,times(1)).doGet(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void doGetReturnsError404WhenCommandNotFound(){
        whenGetCommandNameThenReturn(BAD_COMMAND_PATH);

        try {
            SUT.wrapDoGet(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled();
        verifyError404Called();

    }

    //----------------doPost tests-------------------------
    @Test
    public void doPostRegressionTest(){
        whenGetCommandNameThenReturn(TEST_COMMAND_PATH);

        try {
            SUT.wrapDoPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled();
        try {
            verify(command,times(1)).doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void doPostReturns404WhenCommandNotFound(){
        whenGetCommandNameThenReturn(BAD_COMMAND_PATH);

        try {
            SUT.wrapDoPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verifyGetCommandNameCalled();
        verifyError404Called();
    }

    //----------------getCommandName tests----------------

    @Test
    public void getCommandNameHandlesBadCommandName(){

        when(request.getPathInfo()).thenReturn("/this-command-has-no-trailing-slash");

        Assert.assertEquals("this-command-has-no-trailing-slash", SUT.wrapGetCommandName(request));

        verify(request, times(1)).getPathInfo();
    }

    //------------helpers
    protected void whenGetCommandNameThenReturn(String commandName){
        when(request.getPathInfo()).thenReturn(commandName);
    }
    protected void verifyGetCommandNameCalled(){
        verify(request,times(1)).getPathInfo();
    }
    protected void verifyError404Called(){
        try {
            verify(response,times(1)).sendError(404);
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
