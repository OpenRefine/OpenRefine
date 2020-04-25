package org.openrefine.commands.importing;

import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.CommandTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateImportingJobCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new CreateImportingJobCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
