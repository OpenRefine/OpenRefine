
package com.google.refine.commands.importing;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.CommandTestBase;

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
