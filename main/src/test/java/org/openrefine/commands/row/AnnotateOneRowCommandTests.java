package org.openrefine.commands.row;
import org.openrefine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.row.AnnotateOneRowCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnnotateOneRowCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new AnnotateOneRowCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
