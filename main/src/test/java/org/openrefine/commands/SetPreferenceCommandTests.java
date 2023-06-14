
package org.openrefine.commands;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SetPreferenceCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new SetPreferenceCommand();
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
