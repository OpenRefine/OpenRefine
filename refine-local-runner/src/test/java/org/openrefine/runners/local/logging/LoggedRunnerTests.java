
package org.openrefine.runners.local.logging;

import java.io.IOException;

import org.openrefine.model.Runner;
import org.openrefine.model.RunnerTestBase;
import org.openrefine.model.TestingRunner;

public class LoggedRunnerTests extends RunnerTestBase {

    @Override
    public Runner getDatamodelRunner() throws IOException {
        return new LoggedRunner(new TestingRunner());
    }

}
