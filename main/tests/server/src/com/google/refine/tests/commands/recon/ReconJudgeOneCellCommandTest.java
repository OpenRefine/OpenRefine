package com.google.refine.tests.commands.recon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.commands.recon.ReconJudgeOneCellCommand;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.tests.RefineTest;

public class ReconJudgeOneCellCommandTest extends RefineTest {
        
    Project project = null;
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;
    PrintWriter writer = null;
    
    @BeforeMethod
    public void setUp() {
        project = createCSVProject(
                "reconciled column,unreconciled column\n"+
                "a,b\n"+
                "c,d\n");
        Column reconciled = project.columnModel.columns.get(0);
        ReconConfig config = new StandardReconConfig(
                "http://my.recon.service/api",
                "http://my.recon.service/rdf/space",
                "http://my.recon.service/rdf/schema",
                "type3894", 
                "octopus",
                true,
                Collections.emptyList(),
                5);
        reconciled.setReconConfig(config);
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        
        writer = mock(PrintWriter.class);
        try {
            when(response.getWriter()).thenReturn(writer);
        } catch (IOException e1) {
            Assert.fail();
        }
        
        command = new  ReconJudgeOneCellCommand();
    }
    
    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }
    
    @Test
    public void testMarkOneCellInReconciledColumn() throws Exception {

        when(request.getParameter("row")).thenReturn("0");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("judgment")).thenReturn("new");
        command.doPost(request, response);
        
        Cell cell = project.rows.get(0).cells.get(0);
        Assert.assertEquals(Recon.Judgment.New, cell.recon.judgment);
        Assert.assertEquals("http://my.recon.service/rdf/space", cell.recon.identifierSpace);
    }
    
    @Test
    public void testMarkOneCellWithCustomSpace() throws Exception {

        when(request.getParameter("row")).thenReturn("0");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("judgment")).thenReturn("new");
        when(request.getParameter("identifierSpace")).thenReturn("http://my.custom.space/id");
        when(request.getParameter("schemaSpace")).thenReturn("http://my.custom.space/schema");
        command.doPost(request, response);
        
        Cell cell = project.rows.get(0).cells.get(0);
        Assert.assertEquals(Recon.Judgment.New, cell.recon.judgment);
        Assert.assertEquals("http://my.custom.space/id", cell.recon.identifierSpace);
        Assert.assertEquals("http://my.custom.space/schema", cell.recon.schemaSpace);
    }
}
