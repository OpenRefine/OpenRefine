package com.google.refine.tests.commands.expr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.expr.PreviewExpressionCommand;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class PreviewExpressionCommandTests extends RefineTest {
    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected Command command = null;
    protected StringWriter writer = null;
    
    @BeforeMethod
    public void setUpRequestResponse() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
        command = new PreviewExpressionCommand();
        project = createCSVProject("a,b\nc,d\ne,f\ng,h");
    }
    
    @Test
    public void testJsonResponse() throws ServletException, IOException {

        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("cellIndex")).thenReturn("1");
        when(request.getParameter("expression")).thenReturn("grel:value + \"_u\"");
        when(request.getParameter("rowIndices")).thenReturn("[0,2]");

        String json = "{\n" + 
                "       \"code\" : \"ok\",\n" + 
                "       \"results\" : [ \"d_u\", \"h_u\" ]\n" + 
                "     }";
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(json, writer.toString());
    }
    
    @Test
    public void testParseError() throws ServletException, IOException {

        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("cellIndex")).thenReturn("1");
        when(request.getParameter("expression")).thenReturn("grel:value +");
        when(request.getParameter("rowIndices")).thenReturn("[0,2]");

        String json = "{\n" + 
                "       \"code\" : \"error\",\n" + 
                "       \"message\" : \"Parsing error at offset 7: Expecting something more at end of expression\",\n" + 
                "       \"type\" : \"parser\"\n" + 
                "     }";
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(json, writer.toString());
    }
}
