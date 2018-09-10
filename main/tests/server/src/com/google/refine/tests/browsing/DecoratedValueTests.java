package com.google.refine.tests.browsing;

import java.time.OffsetDateTime;

import org.testng.annotations.Test;

import com.google.refine.browsing.DecoratedValue;
import com.google.refine.tests.util.TestUtils;

public class DecoratedValueTests {
    @Test
    public void serializeDecoratedValue() {
        OffsetDateTime date = OffsetDateTime.parse("2017-03-04T12:56:32Z");
        DecoratedValue dv = new DecoratedValue(date, "[date 2017-03-04T12:56:32Z]");
        TestUtils.isSerializedTo(dv, "{\"v\":\"2017-03-04T12:56:32Z\",\"l\":\"[date 2017-03-04T12:56:32Z]\"}");
    }
}
