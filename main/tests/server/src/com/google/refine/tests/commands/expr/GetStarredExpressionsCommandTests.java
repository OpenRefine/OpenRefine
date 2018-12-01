package com.google.refine.tests.commands.expr;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.expr.GetStarredExpressionsCommand;

public class GetStarredExpressionsCommandTests extends ExpressionCommandTestBase {

    @BeforeMethod
    public void setUp() {
        command = new GetStarredExpressionsCommand();
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
                "         \"code\" : \"grel:cell.recon.match.id\"\n" + 
                "       } ]\n" + 
                "     }";
        command.doGet(request, response);
        assertResponseJsonIs(json);
    }
}
