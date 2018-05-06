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
package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

public class WbDateConstantTest extends WbExpressionTest<TimeValue> {

    private WbDateConstant year = new WbDateConstant("2018");
    private WbDateConstant month = new WbDateConstant("2018-02");
    private WbDateConstant day = new WbDateConstant("2018-02-27");
    private WbDateConstant whitespace = new WbDateConstant("   2018-02-27  ");
    private WbDateConstant hour = new WbDateConstant("2018-02-27T13");

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, year,
                "{\"type\":\"wbdateconstant\",\"value\":\"2018\"}");
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, day,
                "{\"type\":\"wbdateconstant\",\"value\":\"2018-02-27\"}");
    }

    @Test
    public void testEvaluate() {
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 9, 0, 1, 0,
                TimeValue.CM_GREGORIAN_PRO), year);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 10, 0, 1, 0,
                TimeValue.CM_GREGORIAN_PRO), month);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 27, TimeValue.CM_GREGORIAN_PRO), day);
        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 27, (byte) 13, (byte) 0, (byte) 0, (byte) 12, 0, 1,
                0, TimeValue.CM_GREGORIAN_PRO), hour);

        evaluatesTo(Datamodel.makeTimeValue(2018, (byte) 2, (byte) 27, TimeValue.CM_GREGORIAN_PRO), whitespace);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalid() {
        new WbDateConstant("invalid format");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPartlyValid() {
        new WbDateConstant("2018-partly valid");
    }
}
