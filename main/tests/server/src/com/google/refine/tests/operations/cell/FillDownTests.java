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
import com.google.refine.operations.cell.FillDownOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.process.Process;

public class FillDownTests extends RefineTest {
    
    Project project = null;
    
    @BeforeMethod
    public void setUp() {
        project = createCSVProject(
                "key,first,second\n"+
                "a,b,c\n"+
                ",d,\n"+
                "e,f,\n"+
                ",,h\n");
    }
    
    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }
    
    @Test
    public void testFillDownRecordKey() throws Exception {
        AbstractOperation op = new FillDownOperation(
                new JSONObject("{\"mode\":\"record-based\",\"facets\":[]}"),
                "key");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();
        
        Assert.assertEquals("a", project.rows.get(0).cells.get(0).value);
        Assert.assertEquals("a", project.rows.get(1).cells.get(0).value);
        Assert.assertEquals("e", project.rows.get(2).cells.get(0).value);
        Assert.assertEquals("e", project.rows.get(3).cells.get(0).value);
    }
    
    // For issue #742
    // https://github.com/OpenRefine/OpenRefine/issues/742
    @Test
    public void testFillDownRecords() throws Exception {
        AbstractOperation op = new FillDownOperation(
                new JSONObject("{\"mode\":\"record-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();
        
        Assert.assertEquals("c", project.rows.get(0).cells.get(2).value);
        Assert.assertEquals("c", project.rows.get(1).cells.get(2).value);
        Assert.assertNull(project.rows.get(2).cells.get(2));
        Assert.assertEquals("h", project.rows.get(3).cells.get(2).value);
    }
    
    // For issue #742
    // https://github.com/OpenRefine/OpenRefine/issues/742
    @Test
    public void testFillDownRows() throws Exception {       
        AbstractOperation op = new FillDownOperation(
                new JSONObject("{\"mode\":\"row-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();
        
        Assert.assertEquals("c", project.rows.get(0).cells.get(2).value);
        Assert.assertEquals("c", project.rows.get(1).cells.get(2).value);
        Assert.assertEquals("c", project.rows.get(2).cells.get(2).value);
        Assert.assertEquals("h", project.rows.get(3).cells.get(2).value);
    }
}
