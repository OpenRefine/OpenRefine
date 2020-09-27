package org.openrefine.commands.column;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Project;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetColumnsInfoCommandTests extends CommandTestBase {
	
	Project project = null;
	static String jsonResponse = ""
			+ "[ {\n" + 
			"         \"is_numeric\" : false,\n" + 
			"         \"name\" : \"a\",\n" + 
			"         \"numeric_row_count\" : 1,\n" + 
			"         \"other_count\" : 2\n" + 
			"       }, {\n" + 
			"         \"is_numeric\" : true,\n" + 
			"         \"name\" : \"b\",\n" + 
			"         \"numeric_row_count\" : 2,\n" + 
			"         \"other_count\" : 1\n" + 
			"       }, {\n" + 
			"         \"is_numeric\" : true,\n" + 
			"         \"name\" : \"c\",\n" + 
			"         \"numeric_row_count\" : 3,\n" + 
			"         \"other_count\" : 0\n" + 
			"       } ]";
	
	@BeforeMethod
	public void setUpCommand() {
		command = new GetColumnsInfoCommand();
		project = createProject(
				new String[] { "a", "b", "c" },
				new Serializable[] {
					1,   2,   3,
					"a", 4,   5,
					"b", "c", 6
				});
	}
	
	@Test
	public void testGetColumnsInfo() throws ServletException, IOException {
		when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
		
		command.doGet(request, response);
		
		TestUtils.assertEqualAsJson(jsonResponse, writer.toString());
	}
	
}
