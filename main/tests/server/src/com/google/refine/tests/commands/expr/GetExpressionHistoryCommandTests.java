package com.google.refine.tests.commands.expr;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.expr.GetExpressionHistoryCommand;

public class GetExpressionHistoryCommandTests extends ExpressionCommandTestBase {
    
    @BeforeMethod
    public void setUp() {
        command = new GetExpressionHistoryCommand();
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
                "         \"code\" : \"grel:facetCount(value, 'value', 'Column 1')\",\n" + 
                "         \"global\" : false,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"code\" : \"grel:facetCount(value, 'value', 'Column 3')\",\n" + 
                "         \"global\" : false,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"code\" : \"grel:cell.recon.match.id\",\n" + 
                "         \"global\" : false,\n" + 
                "         \"starred\" : true\n" + 
                "       } ]\n" + 
                "     }";
        command.doGet(request, response);
        assertResponseJsonIs(json);
    }

}
