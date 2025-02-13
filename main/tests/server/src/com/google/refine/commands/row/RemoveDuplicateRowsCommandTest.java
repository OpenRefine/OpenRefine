
package com.google.refine.commands.row;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.CommandTestBase;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.Project;

public class RemoveDuplicateRowsCommandTest extends CommandTestBase {

    private Project project = null;
    protected RemoveDuplicateRowsCommand command;

    @BeforeMethod
    public void setUpCommand() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        command = new RemoveDuplicateRowsCommand();
    }

    @Test
    // If CSRF token is missing, respond with CSRF error
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

}
