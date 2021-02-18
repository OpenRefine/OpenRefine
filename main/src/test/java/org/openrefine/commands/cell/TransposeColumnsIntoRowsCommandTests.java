package org.openrefine.commands.cell;

import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.CommandTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TransposeColumnsIntoRowsCommandTests extends CommandTestBase {
	@BeforeMethod
	public void setUpCommand() {
		command = new TransposeColumnsIntoRowsCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
