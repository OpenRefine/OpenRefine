
package org.openrefine.runners.spark;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.Runner;
import org.openrefine.model.RunnerTestBase;

/**
 * This runs the common test suite of all runners. Tests are added by inheritance, from {@link RunnerTestBase}
 * 
 * @author Antonin Delpeuch
 *
 */
public class SparkRunnerTests extends RunnerTestBase {

    @Override
    public Runner getDatamodelRunner() {
        return new SparkRunner(SparkBasedTest.context);
    }

    // Additional tests specific to the Spark implementation

    @Test
    public void testGridFromMemoryHasCachedRowCount() {
        SparkGrid grid = (SparkGrid) createGrid(new String[] { "foo" }, new Serializable[][] { { "bar" } });

        Assert.assertTrue(grid.isRowCountCached());
        Assert.assertEquals(grid.rowCount(), 1L);
    }

}
