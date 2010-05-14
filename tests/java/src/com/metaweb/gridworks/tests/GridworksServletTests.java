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
    final static protected Logger logger = LoggerFactory.getLogger("CancelProcessesCommandTests");

    //System under test
    GridworksServletStub SUT = null;

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
        GridworksServletStub.InsertTestCommand(command); //inject mock into command container
    }

    @After
    public void TearDown(){
        SUT = null;
        request = null;
        response = null;
        command = null;
        GridworksServletStub.RemoveTestCommand(); //remove mock to clean command container
    }

    //--------------------doGet tests---------------------
    @Test
    public void doGetRegressionTest(){
        when(request.getPathInfo()).thenReturn("/test-command/foobar");  //called in getCommandName method

        try {
            SUT.wrapDoGet(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verify(request,times(1)).getPathInfo();
        try {
            verify(command,times(1)).doGet(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
