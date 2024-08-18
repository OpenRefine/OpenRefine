
package com.google.refine.commands.project;

import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;

public class RenameProjectCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new RenameProjectCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testNoProjectId() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        command.doPost(request, response);
        assertErrorNotCSRF();
    }

}
