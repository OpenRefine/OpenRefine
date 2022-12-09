
package org.openrefine.model.local.util.logging;

import java.io.IOException;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.DatamodelRunnerTestBase;
import org.openrefine.model.TestingDatamodelRunner;
import org.openrefine.model.local.util.logging.LoggedDatamodelRunner;

public class LoggedDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() throws IOException {
        return new LoggedDatamodelRunner(new TestingDatamodelRunner());
    }

}
