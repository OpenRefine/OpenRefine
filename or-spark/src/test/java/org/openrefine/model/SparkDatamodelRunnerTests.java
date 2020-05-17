
package org.openrefine.model;

import org.openrefine.SparkBasedTest;

public class SparkDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return new SparkDatamodelRunner(SparkBasedTest.context);
    }

}
