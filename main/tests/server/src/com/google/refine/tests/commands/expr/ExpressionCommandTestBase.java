package com.google.refine.tests.commands.expr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.commands.Command;
import com.google.refine.io.FileProjectManager;
import com.google.refine.tests.util.TestUtils;

public class ExpressionCommandTestBase {
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
    }
    
    public void initWorkspace(String expressionsJson, String starredExpressionsJson) {
        String starred = starredExpressionsJson == null ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647," +
                "\"list\":[]}" : starredExpressionsJson;
        String expressions = expressionsJson == null ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}" : expressionsJson;
        try {
            File workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
            File jsonPath = new File(workspaceDir, "workspace.json");
            FileUtils.writeStringToFile(jsonPath, "{\"projectIDs\":[]\n" + 
                    ",\"preferences\":{\"entries\":{\"scripting.starred-expressions\":" + starred +
                    ",\"scripting.expressions\":"+expressions+"}}}");
            FileProjectManager.initialize(workspaceDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void assertResponseJsonIs(String expectedJson)  {
        String actualJson = writer.toString();
        if(!TestUtils.equalAsJson(expectedJson, actualJson)) {
            try {
                TestUtils.jsonDiff(expectedJson, actualJson);
            } catch (JsonParseException | JsonMappingException e) {
                e.printStackTrace();
            }
        }
        TestUtils.assertEqualAsJson(expectedJson, actualJson);
    }
    
}
