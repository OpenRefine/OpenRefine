package com.google.refine.commands;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

public class OpenWorkspaceDirCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new OpenWorkspaceDirCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}

