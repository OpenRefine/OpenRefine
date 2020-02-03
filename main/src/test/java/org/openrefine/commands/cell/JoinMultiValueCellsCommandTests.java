package org.openrefine.commands.cell;

import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.commands.cell.JoinMultiValueCellsCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;

public class JoinMultiValueCellsCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new JoinMultiValueCellsCommand();
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
}
