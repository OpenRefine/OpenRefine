
package org.openrefine.commands.importing;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.importing.ImportingControllerCommand;

public class ImportingControllerCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new ImportingControllerCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
