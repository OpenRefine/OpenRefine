
package org.openrefine.commands.project;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;

public class RenameProjectCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new RenameProjectCommand();
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testNoProjectId() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        assertThrows(ServletException.class, () -> command.doPost(request, response));
    }
}
