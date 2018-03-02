package org.openrefine.wikidata.commands;

import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.util.ParsingUtilities;

public abstract class CommandTest extends RefineTest {
    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    
    protected Command command = null;
    
    @BeforeMethod(alwaysRun = true)
    public void setUpProject() throws JSONException {
        project = createCSVProject(TestingData.inceptionWithNewCsv);
        TestingData.reconcileInceptionCells(project);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        
        try {
            when(response.getWriter()).thenReturn(printWriter);
        } catch (IOException e1) {
            Assert.fail();
        }
    }
    
    
}
