package com.metaweb.gridworks.tests.commands.util;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.commands.util.CancelProcessesCommand;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.ProcessManager;

public class CancelProcessesCommandTests {

    // logging
    final static protected Logger logger = LoggerFactory.getLogger("CancelProcessesCommandTests");

    // System Under Test
    CancelProcessesCommand SUT = null;

    // variables
    long PROJECT_ID_LONG = 1234;
    String PROJECT_ID = "1234";

    // mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    ProjectManager projMan = null;
    Project proj = null;
    ProcessManager processMan = null;
    PrintWriter pw = null;

    @BeforeMethod
    public void SetUp() {
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        proj = mock(Project.class);
        processMan = mock(ProcessManager.class);
        pw = mock(PrintWriter.class);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        SUT = new CancelProcessesCommand();
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;

        projMan = null;
        ProjectManager.singleton = null;
        proj = null;
        pw = null;
        request = null;
        response = null;
    }

    @Test
    public void doPostFailsThrowsWithNullParameters() {

        // both parameters null
        try {
            SUT.doPost(null, null);
            Assert.fail(); // should have thrown exception by this point
        } catch (IllegalArgumentException e){
            //expected
        } catch (ServletException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }

        // request is null
        try {
            SUT.doPost(null, response);
            Assert.fail(); // should have thrown exception by this point
        } catch (IllegalArgumentException e){
            //expected
        } catch (ServletException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }

        // response parameter null
        try {
            SUT.doPost(request, null);
            Assert.fail(); // should have thrown exception by this point
        } catch (IllegalArgumentException e){
            // expected
        } catch (ServletException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     *  Contract for a complete working post
     */
    @Test
    public void doPostRegressionTest() {

        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(projMan.getProject(anyLong())).thenReturn(proj);
        when(proj.getProcessManager()).thenReturn(processMan);
        try {
            when(response.getWriter()).thenReturn(pw);
        } catch (IOException e1) {
            Assert.fail();
        }

        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("project");
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);

        verify(processMan, times(1)).cancelAll();
        verify(response, times(1)).setCharacterEncoding("UTF-8");
        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        verify(proj, times(1)).getProcessManager();
        try {
            verify(response, times(1)).getWriter();
        } catch (IOException e) {
            Assert.fail();
        }
        verify(pw, times(1)).write("{ \"code\" : \"ok\" }");
    }

     @Test
     public void doPostThrowsIfCommand_getProjectReturnsNull(){
        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(projMan.getProject(anyLong()))
            .thenReturn(null);

        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            //expected
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("project");
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);
     }

     @Test
     public void doPostCatchesExceptionFromWriter(){
         String ERROR_MESSAGE = "hello world";

        // mock dependencies
            when(request.getParameter("project")).thenReturn(PROJECT_ID);
            when(projMan.getProject(anyLong())).thenReturn(proj);
            when(proj.getProcessManager()).thenReturn(processMan);
            try {
                when(response.getWriter()).thenThrow(new IllegalStateException(ERROR_MESSAGE))
                .thenReturn(pw);
            } catch (IOException e) {
                Assert.fail();
            }

            // run
            try {
                SUT.doPost(request, response);
            } catch (ServletException e) {
                Assert.fail();
            } catch (IOException e) {
                Assert.fail();
            }

            verify(request, times(1)).getParameter("project");
            verify(projMan, times(1)).getProject(PROJECT_ID_LONG);

            verify(processMan, times(1)).cancelAll();
            verify(response, times(3)).setCharacterEncoding("UTF-8");
            //omitted other verifications for brevity.
            //assumption is that expecting response.setCharacterEncoding times(3)
            //implies it has Command.respondException has been called as expected
     }
}
