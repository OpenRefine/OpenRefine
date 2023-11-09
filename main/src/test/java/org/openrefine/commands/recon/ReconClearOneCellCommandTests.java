
package org.openrefine.commands.recon;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.recon.ReconClearOneCellCommand;

public class ReconClearOneCellCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new ReconClearOneCellCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
