
package org.openrefine.commands.column;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.column.MoveColumnCommand;

public class MoveColumnCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new MoveColumnCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
