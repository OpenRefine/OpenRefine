package org.openrefine.commands.column;

import org.openrefine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.column.RenameColumnCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RenameColumnCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new RenameColumnCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
