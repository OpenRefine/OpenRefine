package com.google.refine.commands.history;

import com.google.refine.commands.CommandTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

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
