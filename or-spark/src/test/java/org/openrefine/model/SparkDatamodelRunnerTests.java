
package org.openrefine.model;

import org.openrefine.SparkBasedTest;

/**
 * This runs the common test suite of all datamodel runners. Tests are added by inheritance.
 * 
 * @author Antonin Delpeuch
 *
 */
public class SparkDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return new SparkDatamodelRunner(SparkBasedTest.context);
    }

}
