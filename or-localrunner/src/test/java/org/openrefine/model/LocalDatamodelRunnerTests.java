
package org.openrefine.model;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.io.OrderedLocalFileSystem;

/**
 * Tests for this datamodel implementation are taken from the standard test suite, in {@link DatamodelRunnerTestBase}.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LocalDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() throws IOException {
        return new LocalDatamodelRunner();
    }

    @Test
    public void testOrderedFileSystem() throws IOException {
        Assert.assertTrue(SUT.getFileSystem() instanceof OrderedLocalFileSystem);
    }

}
