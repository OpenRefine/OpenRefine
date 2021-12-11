
package org.openrefine.model.local;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class Tuple2Tests {

    Tuple2<Integer, String> tuple;

    @BeforeTest
    public void setUp() {
        tuple = Tuple2.of(3, "foo");
    }

    @Test
    public void testAccessors() {
        Assert.assertEquals((int) tuple.getKey(), 3);
        Assert.assertEquals(tuple.getValue(), "foo");
    }

    @Test
    public void testEquals() {
        Assert.assertNotEquals(tuple, 3);
        Assert.assertEquals(tuple, new Tuple2<Integer, String>(3, "foo"));
    }

    @Test
    public void testToString() {
        Assert.assertEquals(tuple.toString(), "(3, foo)");
        Assert.assertEquals(Tuple2.of((Integer) null, 3).toString(), "(null, 3)");
        Assert.assertEquals(Tuple2.of(3, (Integer) null).toString(), "(3, null)");
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(tuple.hashCode(), Tuple2.of(3, "foo").hashCode());
    }
}
