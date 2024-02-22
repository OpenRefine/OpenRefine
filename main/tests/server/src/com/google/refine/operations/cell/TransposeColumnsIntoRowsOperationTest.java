
package com.google.refine.operations.cell;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;

public class TransposeColumnsIntoRowsOperationTest extends RefineTest {

    Project project = null;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "transpose-columns-into-rows", TransposeColumnsIntoRowsOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        project = createProject(
                new String[] { "num1", "num2" },
                new Serializable[][] {
                        { "2", "3" },
                        { "6", null },
                        { "5", "9" }
                });
    }

    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }

    @Test
    public void testCreateHistoryEntry_transposeIntoOneColumn_removeRowForNullOrEmptyCell() throws Exception {
        AbstractOperation op = new TransposeColumnsIntoRowsOperation("num1", -1, true, false, "a", true, ":");

        runOperation(op, project);

        Assert.assertEquals("num1:2", project.rows.get(0).cells.get(0).value);
        Assert.assertEquals("num2:3", project.rows.get(1).cells.get(0).value);
        Assert.assertEquals("num1:6", project.rows.get(2).cells.get(0).value);
        Assert.assertEquals("num1:5", project.rows.get(3).cells.get(0).value);
        Assert.assertEquals("num2:9", project.rows.get(4).cells.get(0).value);
    }
}
