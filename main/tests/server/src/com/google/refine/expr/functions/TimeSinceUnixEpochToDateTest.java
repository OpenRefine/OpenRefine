
package com.google.refine.expr.functions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Properties;
import org.testng.annotations.Test;
import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;

public class TimeSinceUnixEpochToDateTest extends RefineTest {

    long epoch = 1650547184707L; // 2022-04-21T13:19:44.707Z
    static Properties bindings = new Properties();

    @Test
    public void testTimeSinceUnixEpochToDateOneParam() {
        long epoch1 = epoch / 1000;
        TimeSinceUnixEpochToDate etd = new TimeSinceUnixEpochToDate();
        assertEquals(etd.call(bindings, new Object[] { epoch1 }).toString(), "2022-04-21T13:19:44Z");
    }

    @Test
    public void testTimeSinceUnixEpochToDateTwoParam() {
        long epochSecond = epoch / 1000;
        long epochMilliSecond = epoch;
        long epochMicroSecond = epoch * 1000 + 123;
        TimeSinceUnixEpochToDate etd = new TimeSinceUnixEpochToDate();
        assertEquals(etd.call(bindings, new Object[] { epochSecond, "second" }).toString(), "2022-04-21T13:19:44Z");
        assertEquals(etd.call(bindings, new Object[] { epochMilliSecond, "millisecond" }).toString(), "2022-04-21T13:19:44.707Z");
        assertEquals(etd.call(bindings, new Object[] { epochMicroSecond, "microsecond" }).toString(), "2022-04-21T13:19:44.707123Z");
    }

    @Test
    public void testDescriptionParamsReturns() {
        long epochMilliSecond = epoch;
        TimeSinceUnixEpochToDate etd = new TimeSinceUnixEpochToDate();
        assertEquals(etd.getDescription(),
                "Returns a number converted to a date based on Unix Epoch Time. The number can be Unix Epoch Time in one of the following supported units: second, millisecond, microsecond. Defaults to 'second'.");
        assertEquals(etd.getParams(),
                "number n, string unit (optional, defaults to 'seconds')");
        assertEquals(etd.getReturns(), "date(OffsetDateTime)");
        assertTrue(etd.call(bindings, new Object[] { "millisecond", epochMilliSecond }) instanceof EvalError);
    }

    @Test
    public void testTimeSinceUnixEpochToDateEvalError() {
        long epochMilliSecond = epoch;
        TimeSinceUnixEpochToDate etd = new TimeSinceUnixEpochToDate();

        assertTrue(etd.call(bindings, new Object[] { "millisecond", epochMilliSecond }) instanceof EvalError);
    }

}
