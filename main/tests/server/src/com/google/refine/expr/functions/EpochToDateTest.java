
package com.google.refine.expr.functions;

import static org.testng.Assert.assertEquals;
import java.util.Properties;
import org.testng.annotations.Test;
import com.google.refine.RefineTest;

public class EpochToDateTest extends RefineTest {

    long epoch = 1485105822000L; // 2017-01-22T11:23:42-06:00
    static Properties bindings = new Properties();

    @Test
    public void testEpoch2DateOneParam() {
        long epoch1 = epoch / 1000;
        EpochToDate etd = new EpochToDate();
        assertEquals(etd.call(bindings, new Object[] { epoch1 }).toString(), "2017-01-22T11:23:42-06:00");
    }

    @Test
    public void testEpoch2DateTwoParam() {
        long epochSecond = epoch / 1000;
        long epochMilliSecond = epoch;
        long epochMicroSecond = epoch * 1000;
        EpochToDate etd = new EpochToDate();
        assertEquals(etd.call(bindings, new Object[] { epochSecond, "second" }).toString(), "2017-01-22T11:23:42-06:00");
        assertEquals(etd.call(bindings, new Object[] { epochMilliSecond, "millisecond" }).toString(), "2017-01-22T11:23:42-06:00");
        assertEquals(etd.call(bindings, new Object[] { epochMicroSecond, "microsecond" }).toString(), "2017-01-22T11:23:42-06:00");
    }

}
