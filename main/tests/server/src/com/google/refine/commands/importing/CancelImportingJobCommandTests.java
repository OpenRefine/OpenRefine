package com.google.refine.commands.importing;

import com.google.refine.commands.CommandTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

public class CancelImportingJobCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new CancelImportingJobCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
