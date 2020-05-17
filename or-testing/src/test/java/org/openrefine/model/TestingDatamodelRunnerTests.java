
package org.openrefine.model;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.DatamodelRunnerTestBase;
import org.openrefine.model.TestingDatamodelRunner;

public class TestingDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return new TestingDatamodelRunner();
    }

}
