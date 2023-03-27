
package org.openrefine.runners.local;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.runners.local.pll.util.TaskSignalling;

public class LocalChangeDataTests {

    @Test
    public void testWrapStreamWithProgressReporting() {
        List<Tuple2<Long, Integer>> list = Arrays.asList(
                Tuple2.of(32L, 1),
                Tuple2.of(45L, 2));

        TaskSignalling taskSignalling = new TaskSignalling(100L);
        Stream<Tuple2<Long, Integer>> stream = LocalChangeData.wrapStreamWithProgressReporting(30L, list.stream(), taskSignalling);

        Iterator<Tuple2<Long, Integer>> iterator = stream.iterator();

        Assert.assertEquals(taskSignalling.getProgress(), 0);
        Assert.assertEquals(iterator.next(), Tuple2.of(32L, 1));
        Assert.assertEquals(taskSignalling.getProgress(), 2);
        Assert.assertEquals(iterator.next(), Tuple2.of(45L, 2));
        Assert.assertEquals(taskSignalling.getProgress(), 15);
        Assert.assertFalse(iterator.hasNext());
    }
}
