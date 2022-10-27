/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import java.util.Calendar;

import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

public class WbDateConstantTest extends WbExpressionTest<TimeValue> {

    private WbDateConstant millenium = new WbDateConstant("1001M");
    private WbDateConstant century = new WbDateConstant("1701C");
    private WbDateConstant decade = new WbDateConstant("1990D");
    private WbDateConstant year = new WbDateConstant("2018");
    private WbDateConstant month = new WbDateConstant("2018-02");
    private WbDateConstant day = new WbDateConstant("2018-02-27");
    private WbDateConstant whitespace = new WbDateConstant("   2018-02-27  ");
    private WbDateConstant second = new WbDateConstant("2017-01-03T04:12:45");
    private WbDateConstant secondz = new WbDateConstant("2017-01-03T04:12:45Z");

    private WbDateConstant julianDay = new WbDateConstant("1324-02-27_Q1985786");
    private WbDateConstant julianMonth = new WbDateConstant("1324-02_Q1985786");
    private WbDateConstant julianYear = new WbDateConstant("1324_Q1985786");
    private WbDateConstant julianDecade = new WbDateConstant("1320D_Q1985786");

    private WbDateConstant BCEmillenium = new WbDateConstant("-1001M");
    private WbDateConstant BCEcentury = new WbDateConstant("-1701C");
    private WbDateConstant BCEdecade = new WbDateConstant("-1990D");
    private WbDateConstant BCEyear = new WbDateConstant("-2018");
    private WbDateConstant BCEmonth = new WbDateConstant("-2018-02");
    private WbDateConstant BCEday = new WbDateConstant("-2018-02-27");
    private WbDateConstant BCEwhitespace = new WbDateConstant("   -2018-02-27  ");
    private WbDateConstant BCEsecond = new WbDateConstant("-2017-01-03T04:12:45");
    private WbDateConstant BCEsecondz = new WbDateConstant("-2017-01-03T04:12:45Z");

    private WbDateConstant BCEjulianDay = new WbDateConstant("-1324-02-27_Q1985786");
    private WbDateConstant BCEjulianMonth = new WbDateConstant("-1324-02_Q1985786");
    private WbDateConstant BCEjulianYear = new WbDateConstant("-1324_Q1985786");
    private WbDateConstant BCEjulianDecade = new WbDateConstant("-1320D_Q1985786");

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, year,
                "{\"type\":\"wbdateconstant\",\"value\":\"2018\"}");
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, day,
                "{\"type\":\"wbdateconstant\",\"value\":\"2018-02-27\"}");
    }

    @Test
    public void testEvaluate() {

        evaluatesTo(Datamodel.makeTimeValue(1001, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 6, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), millenium);
        evaluatesTo(Datamodel.makeTimeValue(1701, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 7, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), century);
        evaluatesTo(Datamodel.makeTimeValue(1990, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 8, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), decade);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), year);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 10, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), month);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), day);
        evaluatesTo(Datamodel.makeTimeValue(2017, (byte) 1, (byte) 3, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), second);
        evaluatesTo(Datamodel.makeTimeValue(2017, (byte) 1, (byte) 3, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), secondz);

        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), whitespace);

        evaluatesTo(Datamodel.makeTimeValue(1320, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 8, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), julianDecade);
        evaluatesTo(Datamodel.makeTimeValue(1324, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), julianYear);
        evaluatesTo(Datamodel.makeTimeValue(1324, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 10, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), julianMonth);
        evaluatesTo(Datamodel.makeTimeValue(1324, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), julianDay);
    }

    @Test
    public void testEvaluateBCE() {
        evaluatesTo(Datamodel.makeTimeValue(-1001, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 6, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEmillenium);
        evaluatesTo(Datamodel.makeTimeValue(-1701, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 7, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEcentury);
        evaluatesTo(Datamodel.makeTimeValue(-1990, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 8, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEdecade);
        evaluatesTo(Datamodel.makeTimeValue(-2018, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEyear);
        evaluatesTo(Datamodel.makeTimeValue(-2018, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 10, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEmonth);
        evaluatesTo(Datamodel.makeTimeValue(-2018, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEday);
        evaluatesTo(Datamodel.makeTimeValue(-2017, (byte) 1, (byte) 3, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEsecond);
        evaluatesTo(Datamodel.makeTimeValue(-2017, (byte) 1, (byte) 3, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEsecondz);

        evaluatesTo(Datamodel.makeTimeValue(-2018, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO), BCEwhitespace);

        evaluatesTo(Datamodel.makeTimeValue(-1320, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 8, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), BCEjulianDecade);
        evaluatesTo(Datamodel.makeTimeValue(-1324, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), BCEjulianYear);
        evaluatesTo(Datamodel.makeTimeValue(-1324, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 10, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), BCEjulianMonth);
        evaluatesTo(Datamodel.makeTimeValue(-1324, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_JULIAN_PRO), BCEjulianDay);
    }

    @Test
    public void testToday() {
        Calendar calendar = Calendar.getInstance();
        TimeValue expectedDate = Datamodel.makeTimeValue(
                calendar.get(Calendar.YEAR),
                (byte) (calendar.get(Calendar.MONTH) + 1),
                (byte) calendar.get(Calendar.DAY_OF_MONTH),
                (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);
        evaluatesTo(expectedDate, new WbDateConstant("TODAY"));
    }

    @Test
    public void testInvalid() {
        hasValidationError("Invalid date provided: 'invalid format'", new WbDateConstant("invalid format"));
    }

    @Test
    public void testPartlyValid() {
        hasValidationError("Invalid date provided: '2018-partly valid'", new WbDateConstant("2018-partly valid"));
    }

    @Test
    public void testInvalidCalendar() {
        hasValidationError("Invalid date provided: '2018-01-02_P234'", new WbDateConstant("2018-01-02_P234"));
    }
}
