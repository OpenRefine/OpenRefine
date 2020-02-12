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

import static org.testng.Assert.assertEquals;

import java.text.ParseException;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

public class WbLocationConstantTest extends WbExpressionTest<GlobeCoordinatesValue> {

    private GlobeCoordinatesValue loc = Datamodel.makeGlobeCoordinatesValue(1.2345, 6.7890,
            WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH);
    private GlobeCoordinatesValue locWithPrecision = Datamodel.makeGlobeCoordinatesValue(1.2345, 6.7890, 0.1,
            GlobeCoordinatesValue.GLOBE_EARTH);
    private String input = "1.2345,6.7890";
    private String inputWithPrecision = "1.2345,6.7890,0.1";

    public void testParseValid()
            throws ParseException {
        assertEquals(loc, WbLocationConstant.parse(input));
        assertEquals(locWithPrecision, WbLocationConstant.parse(inputWithPrecision));
    }

    @Test(expectedExceptions = ParseException.class)
    public void testParseInvalid()
            throws ParseException {
        WbLocationConstant.parse("some bad value");
    }

    @Test
    public void testEvaluate()
            throws ParseException {
        evaluatesTo(loc, new WbLocationConstant(input));
    }

    @Test
    public void testEvaluateWithPrecision()
            throws ParseException {
        evaluatesTo(locWithPrecision, new WbLocationConstant(inputWithPrecision));
    }

    @Test(expectedExceptions = ParseException.class)
    public void constructInvalid()
            throws ParseException {
        new WbLocationConstant("some bad value");
    }

    @Test(expectedExceptions = ParseException.class)
    public void constructNotNumber()
            throws ParseException {
        new WbLocationConstant("lat,lng");
    }

    @Test(expectedExceptions = ParseException.class)
    public void constructtooManyParts()
            throws ParseException {
        new WbLocationConstant("0.1,2.3,4.5,6.7");
    }

    @Test
    public void testSerialization()
            throws ParseException {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, new WbLocationConstant(input),
                "{\"type\":\"wblocationconstant\",\"value\":\"" + input + "\"}");
    }
}
