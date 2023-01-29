
package org.openrefine.model;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.SparkBasedTest;

/**
 * This runs the common test suite of all datamodel runners. Tests are added by inheritance, from
 * {@link DatamodelRunnerTestBase}
 * 
 * @author Antonin Delpeuch
 *
 */
public class SparkDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return new SparkDatamodelRunner(SparkBasedTest.context);
    }

    // Additional tests specific to the Spark implementation

    @Test
    public void testGridFromMemoryHasCachedRowCount() {
        SparkGrid grid = (SparkGrid) createGrid(new String[] { "foo" }, new Serializable[][] { { "bar" } });

        Assert.assertTrue(grid.isRowCountCached());
        Assert.assertEquals(grid.rowCount(), 1L);
    }

}
