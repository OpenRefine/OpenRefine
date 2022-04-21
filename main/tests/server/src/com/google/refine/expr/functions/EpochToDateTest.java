
package com.google.refine.expr.functions;

import static org.testng.Assert.assertEquals;
import java.util.Properties;
import org.testng.annotations.Test;
import com.google.refine.RefineTest;

public class EpochToDateTest extends RefineTest {

    long epoch = 1650547184707L; // 2022-04-21T13:19:44Z
    static Properties bindings = new Properties();

    @Test
    public void testEpoch2DateOneParam() {
        long epoch1 = epoch / 1000;
        EpochToDate etd = new EpochToDate();
        assertEquals(etd.call(bindings, new Object[] { epoch1 }).toString(), "2022-04-21T13:19:44Z");
    }

    @Test
    public void testEpoch2DateTwoParam() {
        long epochSecond = epoch / 1000;
        long epochMilliSecond = epoch;
        long epochMicroSecond = epoch * 1000;
        EpochToDate etd = new EpochToDate();
        assertEquals(etd.call(bindings, new Object[] { epochSecond, "second" }).toString(), "2022-04-21T13:19:44Z");
        assertEquals(etd.call(bindings, new Object[] { epochMilliSecond, "millisecond" }).toString(), "2022-04-21T13:19:44Z");
        assertEquals(etd.call(bindings, new Object[] { epochMicroSecond, "microsecond" }).toString(), "2022-04-21T13:19:44Z");
    }

}
