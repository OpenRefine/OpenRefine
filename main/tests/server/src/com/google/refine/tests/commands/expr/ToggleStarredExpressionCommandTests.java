package com.google.refine.tests.commands.expr;

import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.expr.ToggleStarredExpressionCommand;

public class ToggleStarredExpressionCommandTests extends ExpressionCommandTestBase {
    
    @BeforeMethod
    public void setUp() {
        command = new ToggleStarredExpressionCommand();
    }
    
    @Test
    public void testJsonResponse() throws ServletException, IOException {

        initWorkspace("{\n" + 
                "        \"class\": \"com.google.refine.preference.TopList\",\n" + 
                "        \"top\": 100,\n" + 
                "        \"list\": [\n" + 
                "          \"grel:facetCount(value, 'value', 'Column 1')\",\n" + 
                "          \"grel:facetCount(value, 'value', 'Column 3')\",\n" + 
                "          \"grel:cell.recon.match.id\"" +
                "]}", "{\n" + 
                        "        \"class\": \"com.google.refine.preference.TopList\",\n" + 
                        "        \"top\": 100,\n" + 
                        "        \"list\": [\n" + 
                        "          \"grel:cell.recon.match.id\"\n" + 
                        "]}");
        
        String json = "{\n" + 
                "       \"expressions\" : [ {\n" + 
                "         \"code\" : \"grel:facetCount(value, 'value', 'Column 1')\"\n" + 
                "       }, {\n" + 
                "         \"code\" : \"grel:cell.recon.match.id\"\n" + 
                "       } ]\n" + 
                "     }";
        when(request.getParameter("expression")).thenReturn("grel:facetCount(value, 'value', 'Column 1')");
        when(request.getParameter("returnList")).thenReturn("yes");
        command.doPost(request, response);
        assertResponseJsonIs(json);
    }
}
