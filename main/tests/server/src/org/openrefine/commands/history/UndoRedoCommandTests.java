package org.openrefine.commands.history;

import org.openrefine.commands.CommandTestBase;
import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.history.UndoRedoCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UndoRedoCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new UndoRedoCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
