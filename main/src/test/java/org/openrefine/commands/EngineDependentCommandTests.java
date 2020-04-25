
package org.openrefine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Project;
import org.openrefine.operations.Operation;

public class EngineDependentCommandTests extends CommandTestBase {

    private static class EngineDependentCommandStub extends EngineDependentCommand {

        @Override
        protected Operation createOperation(Project project, HttpServletRequest request,
                EngineConfig engineConfig) throws Exception {
            return null;
        }

    }

    @BeforeMethod
    public void setUpCommand() {
        command = new EngineDependentCommandStub();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
