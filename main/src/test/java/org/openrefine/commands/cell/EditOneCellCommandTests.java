
package org.openrefine.commands.cell;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.commands.Command;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.util.TestUtils;

public class EditOneCellCommandTests extends RefineTest {

    private static final String PARSABLE_DOUBLE_NUMBER = "12345.123";
    private static final String PARSABLE_LONG_NUMBER = "12345";
    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected Command command = null;
    protected StringWriter writer = null;

    @BeforeMethod
    public void setUpProject() {
        project = createProject(
                new String[] { "first_column", "second_column" },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" } });
        command = new EditOneCellCommand();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEditOneCell() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("string");
        when(request.getParameter("value")).thenReturn("e");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        GridState state = project.getCurrentGridState();
        Row row0 = state.getRow(0);
        assertEquals(row0.cells.get(0).value, "a");
        assertEquals(row0.cells.get(1).value, "b");
        Row row1 = state.getRow(1);
        assertEquals(row1.cells.get(0).value, "e");
        assertEquals(row1.cells.get(1).value, "d");
    }

    @Test
    public void testNumberParsing_parsableLong() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("number");
        when(request.getParameter("value")).thenReturn(PARSABLE_LONG_NUMBER);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        GridState state = project.getCurrentGridState();
        Row row0 = state.getRow(0);
        assertEquals(row0.cells.get(0).value, "a");
        assertEquals(row0.cells.get(1).value, "b");
        Row row1 = state.getRow(1);
        assertTrue(row1.cells.get(0).value instanceof Long);
        assertEquals(row1.cells.get(0).value, new Long(12345));
        assertEquals(row1.cells.get(1).value, "d");
    }

    @Test
    public void testNumberParsing_parsableDouble() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("number");
        when(request.getParameter("value")).thenReturn(PARSABLE_DOUBLE_NUMBER);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        GridState state = project.getCurrentGridState();
        Row row0 = state.getRow(0);
        assertEquals(row0.cells.get(0).value, "a");
        assertEquals(row0.cells.get(1).value, "b");
        Row row1 = state.getRow(1);
        assertTrue(row1.cells.get(0).value instanceof Double);
        assertEquals(row1.cells.get(0).value, 12345.123);
        assertEquals("d", row1.cells.get(1).value);
    }

    @Test
    public void testMissingCSRFToken() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("string");
        when(request.getParameter("value")).thenReturn("e");

        command.doPost(request, response);

        assertEquals("c", project.getCurrentGridState().getRow(1).cells.get(0).value, "c");
        TestUtils.assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }
}
