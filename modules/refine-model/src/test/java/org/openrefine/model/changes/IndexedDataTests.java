
package org.openrefine.model.changes;

import java.util.Iterator;
import java.util.stream.IntStream;

import org.testng.Assert;
import org.testng.annotations.Test;

public class IndexedDataTests {

    @Test
    public void testEquals() {
        Assert.assertNotEquals(new IndexedData<String>(1L, "foo"), 3);
        Assert.assertEquals(new IndexedData<String>(3L, "bar"), new IndexedData<String>(3L, "bar"));
        Assert.assertEquals(new IndexedData<String>(3L, "bar").hashCode(), new IndexedData<String>(3L, "bar").hashCode());
    }

    @Test
    public void testToString() {
        Assert.assertEquals(new IndexedData<String>(1L, "foo").toString(), "[IndexedData 1 foo]");
    }

    @Test
    public void testCompleteIterator() {
        Iterator<IndexedData<String>> originalIterator = IntStream.range(0, 3)
                .mapToObj(i -> new IndexedData<>(i, Integer.toString(i)))
                .iterator();

        Iterator<IndexedData<String>> completed = IndexedData.completeIterator(originalIterator);
        Assert.assertTrue(completed.hasNext());
        Assert.assertEquals(completed.next(), new IndexedData<>(0, "0"));
        Assert.assertTrue(completed.hasNext());
        Assert.assertEquals(completed.next(), new IndexedData<>(1, "1"));
        Assert.assertTrue(completed.hasNext());
        Assert.assertEquals(completed.next(), new IndexedData<>(2, "2"));
        Assert.assertTrue(completed.hasNext());
        Assert.assertEquals(completed.next(), new IndexedData<>(3));
        Assert.assertTrue(completed.hasNext());
        Assert.assertEquals(completed.next(), new IndexedData<>(4));
    }
}
