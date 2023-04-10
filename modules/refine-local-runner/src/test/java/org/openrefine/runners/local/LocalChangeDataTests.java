
package org.openrefine.runners.local;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.runners.local.pll.util.TaskSignalling;
import org.openrefine.util.CloseableIterator;

public class LocalChangeDataTests {

    @Test
    public void testWrapStreamWithProgressReporting() {
        CloseableIterator<Tuple2<Long, Integer>> sourceIterator = CloseableIterator.of(
                Tuple2.of(32L, 1),
                Tuple2.of(45L, 2));

        TaskSignalling taskSignalling = new TaskSignalling(100L);
        CloseableIterator<Tuple2<Long, Integer>> iterator = LocalChangeData.wrapStreamWithProgressReporting(30L, sourceIterator,
                taskSignalling);

        Assert.assertEquals(taskSignalling.getProgress(), 0);
        Assert.assertEquals(iterator.next(), Tuple2.of(32L, 1));
        Assert.assertEquals(taskSignalling.getProgress(), 2);
        Assert.assertEquals(iterator.next(), Tuple2.of(45L, 2));
        Assert.assertEquals(taskSignalling.getProgress(), 15);
        Assert.assertFalse(iterator.hasNext());
    }
}
