
package com.google.refine.commands.row;

import com.google.refine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnnotateOneRowCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new AnnotateOneRowCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
