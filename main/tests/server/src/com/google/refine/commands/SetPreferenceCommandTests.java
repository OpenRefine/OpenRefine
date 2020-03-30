package com.google.refine.commands;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

public class SetPreferenceCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new SetPreferenceCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}

