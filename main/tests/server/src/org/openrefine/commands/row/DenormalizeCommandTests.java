
package org.openrefine.commands.row;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;

public class DenormalizeCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new DenormalizeCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
