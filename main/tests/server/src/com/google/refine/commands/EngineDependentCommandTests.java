
package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class EngineDependentCommandTests extends CommandTestBase {

    private static class EngineDependentCommandStub extends EngineDependentCommand {

        @Override
        protected AbstractOperation createOperation(Project project, HttpServletRequest request,
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
