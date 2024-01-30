
package org.openrefine.process;

import org.testng.Assert;

public class ProgressReporterStub implements ProgressReporter {

    protected int _progress = 0;

    @Override
    public void reportProgress(int percentage, long processedRows, long totalRows) {
        Assert.assertTrue(0 <= percentage && percentage <= 100,
                String.format("Progress %d is out of bounds (0 - 100)", percentage));
        _progress = percentage;
    }

    public int getPercentage() {
        return _progress;
    }

}
