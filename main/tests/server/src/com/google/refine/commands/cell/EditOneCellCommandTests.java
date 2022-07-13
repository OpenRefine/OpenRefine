
package com.google.refine.commands.cell;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.TestUtils;

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
        project = createCSVProject(
                "first_column,second_column\n"
                        + "a,b\n"
                        + "c,d\n");
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
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("string");
        when(request.getParameter("value")).thenReturn("e");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        assertEquals(project.rows.get(0).cells.get(0).value, "a");
        assertEquals(project.rows.get(0).cells.get(1).value, "b");
        assertEquals(project.rows.get(1).cells.get(0).value, "e");
        assertEquals(project.rows.get(1).cells.get(1).value, "d");
    }

    @Test
    public void testNumberParsing_parsableLong() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("number");
        when(request.getParameter("value")).thenReturn(PARSABLE_LONG_NUMBER);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        assertEquals(project.rows.get(0).cells.get(0).value, "a");
        assertEquals(project.rows.get(0).cells.get(1).value, "b");
        assertTrue(project.rows.get(1).cells.get(0).value instanceof Long);
        assertEquals(new Long(12345), project.rows.get(1).cells.get(0).value);
        assertEquals(project.rows.get(1).cells.get(1).value, "d");
    }

    @Test
    public void testNumberParsing_parsableDouble() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("number");
        when(request.getParameter("value")).thenReturn(PARSABLE_DOUBLE_NUMBER);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        assertEquals(project.rows.get(0).cells.get(0).value, "a");
        assertEquals(project.rows.get(0).cells.get(1).value, "b");
        assertTrue(project.rows.get(1).cells.get(0).value instanceof Double);
        assertEquals(project.rows.get(1).cells.get(0).value, 12345.123);
        assertEquals(project.rows.get(1).cells.get(1).value, "d");
    }

    @Test
    public void testMissingCSRFToken() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("row")).thenReturn("1");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("type")).thenReturn("string");
        when(request.getParameter("value")).thenReturn("e");

        command.doPost(request, response);

        assertEquals(project.rows.get(1).cells.get(0).value, "c");
        TestUtils.assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }
}
