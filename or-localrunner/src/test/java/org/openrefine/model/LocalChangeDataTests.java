
package org.openrefine.model;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.local.ConcurrentProgressReporter;
import org.openrefine.model.local.Tuple2;
import org.openrefine.process.ProgressReporterStub;

public class LocalChangeDataTests {

    @Test
    public void testWrapStreamWithProgressReporting() {
        List<Tuple2<Long, Integer>> list = Arrays.asList(
                Tuple2.of(32L, 1),
                Tuple2.of(45L, 2));

        ProgressReporterStub reporter = new ProgressReporterStub();
        ConcurrentProgressReporter concurrentReporter = new ConcurrentProgressReporter(reporter, 100L);
        Stream<Tuple2<Long, Integer>> stream = LocalChangeData.wrapStreamWithProgressReporting(30L, list.stream(), concurrentReporter);

        Iterator<Tuple2<Long, Integer>> iterator = stream.iterator();

        Assert.assertEquals(reporter.getPercentage(), 0);
        Assert.assertEquals(iterator.next(), Tuple2.of(32L, 1));
        Assert.assertEquals(reporter.getPercentage(), 2);
        Assert.assertEquals(iterator.next(), Tuple2.of(45L, 2));
        Assert.assertEquals(reporter.getPercentage(), 15);
        Assert.assertFalse(iterator.hasNext());
    }
}
