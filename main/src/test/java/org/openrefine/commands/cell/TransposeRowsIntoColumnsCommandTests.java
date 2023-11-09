
package org.openrefine.commands.cell;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.cell.TransposeRowsIntoColumnsCommand;

public class TransposeRowsIntoColumnsCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new TransposeRowsIntoColumnsCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
