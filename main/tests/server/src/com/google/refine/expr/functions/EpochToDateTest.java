
package com.google.refine.expr.functions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Properties;
import org.testng.annotations.Test;
import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;

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

    @Test
    public void testDescriptionParamsReturns() {
        long epochMilliSecond = epoch;
        EpochToDate etd = new EpochToDate();
        assertEquals(etd.getDescription(),
                "Returns a number converted to a date. Can parse one parameter or two parameters. When parsing one parameter, the number is the epoch second."
                        + "When parsing two parameters, the first is the number, the second is the numbers type, such as second, millisecond, microsecond.");
        assertEquals(etd.getParams(),
                "A number of epoch second, millisecond, microsecond. The second parameter is not necessary, is the input number's type");
        assertEquals(etd.getReturns(), "date(OffsetDateTime)");
        assertTrue(etd.call(bindings, new Object[] { "millisecond", epochMilliSecond }) instanceof EvalError); // wrong
                                                                                                               // // in
    }

    @Test
    public void testEpoch2DateEvalError() {
        long epochMilliSecond = epoch;
        EpochToDate etd = new EpochToDate();

        assertTrue(etd.call(bindings, new Object[] { "millisecond", epochMilliSecond }) instanceof EvalError); // wrong
                                                                                                               // // in
    }

}
