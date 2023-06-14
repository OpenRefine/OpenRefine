
package org.openrefine.commands.project;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class GetModelsCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUp() {
        command = new GetModelsCommand();
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "c" },
                        { "d", "e" },
                        { "", "f" },
                        { "g", "h" }
                });

        when(request.getParameter("project")).thenReturn(String.valueOf(project.getId()));
    }

    @Test
    public void testCommand() throws Exception {
        String expectedJson = ParsingUtilities.mapper.writeValueAsString(project.getColumnModel());
        command.doGet(request, response);

        verify(response).setStatus(200);
        JsonNode parsedResponse = ParsingUtilities.mapper.readTree(writer.toString());
        TestUtils.assertEqualsAsJson(parsedResponse.get("columnModel").toString(), expectedJson);
    }
}
