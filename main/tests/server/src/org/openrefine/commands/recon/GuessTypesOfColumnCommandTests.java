package org.openrefine.commands.recon;

import org.openrefine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.recon.GuessTypesOfColumnCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GuessTypesOfColumnCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new GuessTypesOfColumnCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
