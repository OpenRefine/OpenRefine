package com.google.refine.commands.cell;

import com.google.refine.commands.CommandTestBase;
import com.google.refine.commands.cell.JoinMultiValueCellsCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

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
