
package org.openrefine.commands.history;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Project;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.TestUtils;

public class CancelProcessesCommandTests extends CommandTestBase {

    // variables
    long PROJECT_ID_LONG = 1234;
    String PROJECT_ID = "1234";

    // mocks
    ProjectManager projMan = null;
    Project proj = null;
    ProcessManager processMan = null;

    @BeforeMethod
    public void SetUp() {
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        proj = mock(Project.class);
        processMan = mock(ProcessManager.class);
        command = new CancelProcessesCommand();
    }

    @AfterMethod
    public void TearDown() {
        command = null;

        projMan = null;
        proj = null;
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void doPostFailsThrowsWithNullParameters() {

        // both parameters null
        try {
            command.doPost(null, null);
            Assert.fail(); // should have thrown exception by this point
        } catch (IllegalArgumentException e) {
            // expected
        } catch (ServletException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }

        // request is null
        try {
            command.doPost(null, response);
            Assert.fail(); // should have thrown exception by this point
        } catch (IllegalArgumentException e) {
            // expected
        } catch (ServletException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }

        // response parameter null
        try {
            command.doPost(request, null);
            Assert.fail(); // should have thrown exception by this point
        } catch (IllegalArgumentException e) {
            // expected
        } catch (ServletException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * Contract for a complete working post
     */
    @Test
    public void doPostRegressionTest() throws Exception {

        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(projMan.getProject(anyLong())).thenReturn(proj);
        when(proj.getProcessManager()).thenReturn(processMan);

        // run
        try {
            command.doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("project");

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
        TestUtils.assertEqualsAsJson(writer.toString(), "{ \"code\" : \"ok\" }");
    }

    @Test
    public void doPostThrowsIfCommand_getProjectReturnsNull() throws Exception {
        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(projMan.getProject(anyLong()))
                .thenReturn(null);

        // run
        try {
            command.doPost(request, response);
        } catch (ServletException e) {
            // expected
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("project");
    }

    @Test
    public void doPostCatchesExceptionFromWriter() throws Exception {
        String ERROR_MESSAGE = "hello world";

        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(projMan.getProject(anyLong())).thenReturn(proj);
        when(proj.getProcessManager()).thenReturn(processMan);

        // run
        try {
            command.doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter("project");

        verify(processMan, times(1)).cancelAll();
        verify(response).setCharacterEncoding("UTF-8");
        // omitted other verifications for brevity.
        // assumption is that expecting response.setCharacterEncoding times(3)
        // implies it has Command.respondException has been called as expected
    }
}
