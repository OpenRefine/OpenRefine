
package com.google.refine.tests.model.changes;

import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.model.changes.ColumnAdditionChange;
import com.google.refine.model.changes.MassChange;
import com.google.refine.history.Change;
import com.google.refine.tests.RefineTest;

public class MassChangeTests extends RefineTest {

    Project project;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp()
            throws IOException, ModelException {
        project = createProjectWithColumns("MassChangeTest");
    }

    /**
     * Test case for #914 - Demonstrates MassChange revert doesn't work by
     * adding two columns to a project with a MassChange and then reverting.
     * Without the fix, column "a" will be removed before column "b", causing
     * column "b" removal to fail because it won't be found at index 1 as
     * expected.
     */
    @Test
    public void testWrongReverseOrder()
            throws Exception {
        List<Change> changes = new ArrayList<Change>();
        changes.add(new ColumnAdditionChange("a", 0, new ArrayList<CellAtRow>()));
        changes.add(new ColumnAdditionChange("b", 1, new ArrayList<CellAtRow>()));
        MassChange massChange = new MassChange(changes, false);
        massChange.apply(project);
        massChange.revert(project);
        assertTrue(project.columnModel.columns.isEmpty());
    }
}
