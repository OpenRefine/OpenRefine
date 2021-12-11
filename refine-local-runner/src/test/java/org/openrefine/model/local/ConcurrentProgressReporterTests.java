
package org.openrefine.model.local;

import org.testng.annotations.Test;

import org.openrefine.process.ProgressReporter;

public class ConcurrentProgressReporterTests {

    @Test
    public void testIncrements() {

        ProgressReporter origReporter = new ProgressReporter() {

            int progress = 0;

            @Override
            public void reportProgress(int percentage) {
                progress = percentage;
            }

        };
        ConcurrentProgressReporter reporter = new ConcurrentProgressReporter(origReporter, 1000);

    }

}
