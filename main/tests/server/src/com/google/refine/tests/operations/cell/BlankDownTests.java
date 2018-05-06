package com.google.refine.tests.operations.cell;

import java.util.Properties;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.cell.BlankDownOperation;
import com.google.refine.process.Process;
import com.google.refine.tests.RefineTest;

public class BlankDownTests extends RefineTest {
    
    Project project = null;
    
    @BeforeMethod
    public void setUp() {
        project = createCSVProject(
                "key,first,second\n"+
                "a,b,c\n"+
                ",d,c\n"+
                "e,f,c\n"+
                ",,c\n");
    }
    
    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }
    
    @Test
    public void testBlankDownRecords() throws Exception {
        AbstractOperation op = new BlankDownOperation(
                new JSONObject("{\"mode\":\"record-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();
        
        Assert.assertEquals("c", project.rows.get(0).cells.get(2).value);
        Assert.assertNull(project.rows.get(1).cells.get(2));
        Assert.assertEquals("c", project.rows.get(2).cells.get(2).value);
        Assert.assertNull(project.rows.get(3).cells.get(2));
    }
    
    @Test
    public void testBlankDownRows() throws Exception {
        AbstractOperation op = new BlankDownOperation(
                new JSONObject("{\"mode\":\"row-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();
        
        Assert.assertEquals("c", project.rows.get(0).cells.get(2).value);
        Assert.assertNull(project.rows.get(1).cells.get(2));
        Assert.assertNull(project.rows.get(2).cells.get(2));
        Assert.assertNull(project.rows.get(3).cells.get(2));
    }
}
