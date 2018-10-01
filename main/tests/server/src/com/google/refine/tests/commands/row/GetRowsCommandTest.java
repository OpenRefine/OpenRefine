package com.google.refine.tests.commands.row;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.row.GetRowsCommand;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class GetRowsCommandTest extends RefineTest {
    
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;
    Project project = null;
    StringWriter writer = null;
    
    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        project = createCSVProject("a,b\nc,d\n,f");
        command = new GetRowsCommand();
        writer = new StringWriter();
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testJsonOutputRows() throws ServletException, IOException {
        String rowJson = "{\n" + 
                "       \"filtered\" : 2,\n" + 
                "       \"limit\" : 2,\n" + 
                "       \"mode\" : \"row-based\",\n" + 
                "       \"pool\" : {\n" + 
                "         \"recons\" : { }\n" + 
                "       },\n" + 
                "       \"rows\" : [ {\n" + 
                "         \"cells\" : [ {\n" + 
                "           \"v\" : \"c\"\n" + 
                "         }, {\n" + 
                "           \"v\" : \"d\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 0,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"cells\" : [ null, {\n" + 
                "           \"v\" : \"f\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 1,\n" + 
                "         \"starred\" : false\n" + 
                "       } ],\n" + 
                "       \"start\" : 0,\n" + 
                "       \"total\" : 2\n" + 
                "     }";
        
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(rowJson, writer.toString());
    }
    
    @Test
    public void testJsonOutputRecords() throws ServletException, IOException {
        String recordJson = "{\n" + 
                "       \"filtered\" : 1,\n" + 
                "       \"limit\" : 2,\n" + 
                "       \"mode\" : \"record-based\",\n" + 
                "       \"pool\" : {\n" + 
                "         \"recons\" : { }\n" + 
                "       },\n" + 
                "       \"rows\" : [ {\n" + 
                "         \"cells\" : [ {\n" + 
                "           \"v\" : \"c\"\n" + 
                "         }, {\n" + 
                "           \"v\" : \"d\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 0,\n" + 
                "         \"j\" : 0,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"cells\" : [ null, {\n" + 
                "           \"v\" : \"f\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 1,\n" + 
                "         \"starred\" : false\n" + 
                "       } ],\n" + 
                "       \"start\" : 0,\n" + 
                "       \"total\" : 1\n" + 
                "     }";
        
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(recordJson, writer.toString());
    }
}
