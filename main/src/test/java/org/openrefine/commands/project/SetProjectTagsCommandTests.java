
package org.openrefine.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.project.SetProjectTagsCommand;

public class SetProjectTagsCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new SetProjectTagsCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
