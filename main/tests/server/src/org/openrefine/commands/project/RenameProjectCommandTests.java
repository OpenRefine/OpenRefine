package org.openrefine.commands.project;

import org.openrefine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.project.RenameProjectCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RenameProjectCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new RenameProjectCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
