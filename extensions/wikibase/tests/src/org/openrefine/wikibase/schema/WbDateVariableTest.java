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

import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.google.refine.model.Cell;

public class WbDateVariableTest extends WbVariableTest<TimeValue> {

    private TimeValue year = Datamodel.makeTimeValue(2018, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9,
            0, 0, 0, TimeValue.CM_GREGORIAN_PRO);
    private TimeValue day = Datamodel.makeTimeValue(2018, (byte) 2, (byte) 27, (byte) 0, (byte) 0, (byte) 0, (byte) 11,
            0, 0, 0, TimeValue.CM_GREGORIAN_PRO);
    private TimeValue minute = Datamodel.makeTimeValue(2001, (byte) 2, (byte) 3, (byte) 0, (byte) 0, (byte) 0, (byte) 11, (byte) 0,
            (byte) 0, (byte) 0, TimeValue.CM_GREGORIAN_PRO);

    @Override
    public WbVariableExpr<TimeValue> initVariableExpr() {
        return new WbDateVariable();
    }

    @Test
    public void testValidFormat() {
        evaluatesTo(year, "2018");
        evaluatesTo(day, "2018-02-27");
    }

    @Test
    public void testWhitespace() {
        evaluatesTo(year, "  2018");
        evaluatesTo(day, "2018-02-27  ");
    }

    @Test
    public void testSkipped() {
        isSkipped("  2018-XX");
        isSkipped("invalid format");
    }

    @Test
    public void testNullStringValue() {
        isSkipped((String) null);
    }

    @Test
    public void testNullCell() {
        isSkipped((Cell) null);
    }

    @Test
    public void testNumber() {
        // numbers are evaluated as years
        evaluatesTo(year, new Cell(2018, null));
        isSkipped(new Cell(1234.56, null));
    }

    @Test
    public void testMinutesISO() {
        // Wikidata currently only supports up to day precision
        evaluatesTo(minute, "2001-02-03T04:05Z");
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, variable,
                "{\"type\":\"wbdatevariable\",\"columnName\":\"column A\"}");
    }

}
