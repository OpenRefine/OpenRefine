
package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenWorkspaceDirCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new OpenWorkspaceDirCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
