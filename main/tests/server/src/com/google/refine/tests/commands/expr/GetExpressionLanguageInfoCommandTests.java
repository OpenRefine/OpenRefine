package com.google.refine.tests.commands.expr;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.commands.expr.GetExpressionLanguageInfoCommand;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class GetExpressionLanguageInfoCommandTests extends ExpressionCommandTestBase {

    @BeforeMethod
    public void setUp() {
        command = new GetExpressionLanguageInfoCommand();
    }
    
    @Test
    public void testJsonResponse() throws ServletException, IOException {

        initWorkspace(null, null);
        
        command.doGet(request, response);
        String jsonResponse = writer.toString();
        JsonNode result = ParsingUtilities.mapper.readValue(jsonResponse, JsonNode.class);
        TestUtils.assertEqualAsJson("{\n" + 
                "           \"description\" : \"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression test which should return a boolean. If the boolean is true, pushes v onto the result array.\",\n" + 
                "           \"params\" : \"expression a, variable v, expression test\",\n" + 
                "           \"returns\" : \"array\"\n" + 
                "         }", result.get("controls").get("filter").toString());
    }
}
