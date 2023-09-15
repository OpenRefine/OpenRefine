
package org.openrefine.model;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RunnerConfigurationTests {

    RunnerConfiguration SUT;

    @BeforeClass
    public void setUp() {
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        map.put("integer-A", "wrong");
        map.put("integer-B", "4");
        map.put("long-A", "wrong");
        map.put("long-B", "3498");
        SUT = new RunnerConfigurationImpl(map);
    }

    @Test
    public void testGetParameter() {
        Assert.assertEquals(SUT.getParameter("foo", "default"), "bar");
        Assert.assertEquals(SUT.getParameter("does-not-exist", "default"), "default");
    }

    @Test
    public void testGetIntParameter() {
        Assert.assertEquals(SUT.getIntParameter("integer-A", 5), 5);
        Assert.assertEquals(SUT.getIntParameter("integer-B", 5), 4);
        Assert.assertEquals(SUT.getIntParameter("does-not-exist", 7), 7);
    }

    @Test
    public void testGetLongParameter() {
        Assert.assertEquals(SUT.getLongParameter("long-A", 5L), 5L);
        Assert.assertEquals(SUT.getLongParameter("long-B", 5L), 3498L);
        Assert.assertEquals(SUT.getLongParameter("does-not-exist", 7L), 7L);
    }
}
