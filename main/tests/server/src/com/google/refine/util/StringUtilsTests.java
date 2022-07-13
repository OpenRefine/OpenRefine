
package com.google.refine.util;

import com.google.refine.RefineTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.time.OffsetDateTime;

public class StringUtilsTests extends RefineTest {

    @Test
    public void objectToString() {
        Object nullObject = null;
        Object[] emptyArray = {};
        Object[] objArray = { 4, "hello", true, 0.01 };
        Object[][] multiArray = { { "OpenRefine", 12 }, { 13, 4.6 }, { "data", "mining" } };
        OffsetDateTime time = OffsetDateTime.parse("2017-01-02T01:02:03Z");

        Assert.assertEquals("", StringUtils.toString(nullObject));
        Assert.assertEquals("[]", StringUtils.toString(emptyArray));
        Assert.assertEquals("[4, hello, true, 0.01]", StringUtils.toString(objArray));
        Assert.assertEquals("[[OpenRefine, 12], [13, 4.6], [data, mining]]", StringUtils.toString(multiArray));
        Assert.assertEquals("2017-01-02T01:02:03Z", StringUtils.toString(time));
    }
}
