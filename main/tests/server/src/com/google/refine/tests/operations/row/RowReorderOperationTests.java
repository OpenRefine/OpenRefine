package com.google.refine.tests.operations.row;

import java.util.Properties;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowReorderOperation;
import com.google.refine.process.Process;
import com.google.refine.tests.RefineTest;

public class RowReorderOperationTests extends RefineTest {
    
    Project project = null;
    
    @BeforeMethod
    public void setUp() {
        project = createCSVProject(
                "key,first\n"+
                "8,b\n"+
                ",d\n"+
                "2,f\n"+
                "1,h\n");
    }
    
    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }
    
    @Test
    public void testSortEmptyString() throws Exception {
        project.rows.get(1).cells.set(0, new Cell("", null));
        AbstractOperation op = new RowReorderOperation(
                Mode.RowBased,
                new JSONObject("{\"criteria\":[{\"column\":\"key\",\"valueType\":\"number\",\"reverse\":false,\"blankPosition\":2,\"errorPosition\":1}]}"));
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();
        
        Assert.assertEquals("h", project.rows.get(0).cells.get(1).value);
        Assert.assertEquals("f", project.rows.get(1).cells.get(1).value);
        Assert.assertEquals("b", project.rows.get(2).cells.get(1).value);
        Assert.assertEquals("d", project.rows.get(3).cells.get(1).value);
    }
}