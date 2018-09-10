package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.ToDate;
import com.google.refine.tests.util.TestUtils;

public class ToDateTests {
    @Test
    public void serializeToDate() {
        String json = "{\"description\":\"Returns o converted to a date object, you can hint if the day or the month is listed first, or give an ordered list of possible formats using this syntax: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html\",\"params\":\"o, boolean month_first / format1, format2, ... (all optional)\",\"returns\":\"date\"}";
        TestUtils.isSerializedTo(new ToDate(), json);
    }
}

