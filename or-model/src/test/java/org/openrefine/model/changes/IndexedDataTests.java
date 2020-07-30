package org.openrefine.model.changes;

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
}
