
package org.openrefine.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for this datamodel implementation are taken from the standard test suite, in {@link DatamodelRunnerTestBase}.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LocalDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() throws IOException {
        Map<String, String> map = new HashMap<>();
        // these values are purposely very low for testing purposes,
        // so that we can check the partitioning strategy without using large files
        map.put("minSplitSize", "128");
        map.put("maxSplitSize", "1024");

        RunnerConfiguration runnerConf = new RunnerConfigurationImpl(map);
        return new LocalDatamodelRunner(runnerConf);
    }

}
