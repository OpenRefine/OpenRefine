package com.google.gridworks.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.gridworks.commands.Command;

public class GridworksServletTests extends GridworksTest {
    
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
        
    //System under test
    GridworksServletStub SUT = null;

    //variables
    final static private String TEST_COMMAND_NAME = "test-command";
    final static private String TEST_COMMAND_PATH = "/command/core/test-command/foobar";
    final static private String BAD_COMMAND_PATH = "/command-does-not-exist";

    final static private String POST = "POST";
    final static private String GET = "GET";
    
    // mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;


    @BeforeMethod
    public void SetUp() throws ServletException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = mock(Command.class);

        SUT = new GridworksServletStub();
        SUT.insertCommand(TEST_COMMAND_NAME,command); //inject mock into command container
    }

    @AfterMethod
    public void TearDown() {
        SUT.removeCommand(TEST_COMMAND_NAME); //remove mock to clean command container
        SUT = null;
        
        request = null;
        response = null;
        command = null;
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
        whenGetMethodThenReturn(GET);

        try {
            SUT.wrapService(request, response);
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
        whenGetMethodThenReturn(GET);

        try {
            SUT.wrapService(request, response);
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
        whenGetMethodThenReturn(POST);

        try {
            SUT.wrapService(request, response);
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
        whenGetMethodThenReturn(POST);
        
        try {
            SUT.wrapService(request, response);
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

        when(request.getPathInfo()).thenReturn("/command/this-command-has-no-trailing-slash");

        Assert.assertEquals("this-command-has-no-trailing-slash", SUT.wrapGetCommandName(request));

        verify(request, times(1)).getPathInfo();
    }

    //------------helpers
    protected void whenGetCommandNameThenReturn(String commandName){
        when(request.getPathInfo()).thenReturn(commandName);
    }
    protected void whenGetMethodThenReturn(String method){
        when(request.getMethod()).thenReturn(method);
    }
    protected void verifyGetCommandNameCalled(){
        verify(request,times(2)).getPathInfo();
    }
    protected void verifyError404Called(){
        try {
            verify(response,times(1)).sendError(404);
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
