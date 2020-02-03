package org.openrefine.commands.project;

import org.openrefine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.project.ImportProjectCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ImportProjectCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new ImportProjectCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}

