
package org.openrefine.runners.local.logging;

import org.openrefine.model.Runner;
import org.openrefine.runners.testing.RunnerTestBase;
import org.openrefine.runners.testing.TestingRunner;

import java.io.IOException;

public class LoggedRunnerTests extends RunnerTestBase {

    @Override
    public Runner getDatamodelRunner() throws IOException {
        return new LoggedRunner(new TestingRunner());
    }

}
