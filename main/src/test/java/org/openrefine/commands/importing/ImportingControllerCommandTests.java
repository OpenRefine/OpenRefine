
package org.openrefine.commands.importing;

import org.openrefine.commands.CommandTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ImportingControllerCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new ImportingControllerCommand();
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
