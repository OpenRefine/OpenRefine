
package org.openrefine.commands.project;

import org.openrefine.commands.CommandTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
}
