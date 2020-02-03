package com.google.refine.commands.expr;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;

public class LogExpressionCommandTests extends CommandTestBase {
	
	PreferenceStore prefStore;
	
	@BeforeMethod
	public void setUpCommand() {
		command = new LogExpressionCommand();
		prefStore = ProjectManager.singleton.getPreferenceStore();
	}

	@Override
	protected void cleanupProjectsAndJobs() {
		// do nothing, since ProjectManager.singleton was overriden by us
	}
	
	@Test
	public void testCSRFProtection() throws ServletException, IOException {
		command.doPost(request, response);
		assertCSRFCheckFailed();
	}
	
	@Test
	public void testNullExpressions() throws ServletException, IOException {
		prefStore.put("scripting.expressions", null);
		when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
		when(request.getParameter("expression")).thenReturn("grel:value+'a'");
		
		command.doPost(request, response);
		
		TopList topList = (TopList)prefStore.get("scripting.expressions");
		Assert.assertEquals(topList.getList(), Collections.singletonList("grel:value+'a'"));
	}
}
