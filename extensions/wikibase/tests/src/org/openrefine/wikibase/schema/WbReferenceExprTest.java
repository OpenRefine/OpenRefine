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

import java.util.Arrays;
import java.util.Collections;

import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbReferenceExprTest extends WbExpressionTest<Reference> {

    private WbReferenceExpr expr = new WbReferenceExpr(Arrays.asList(
            new WbSnakExpr(new WbPropConstant("P87", "retrieved", "time"), new WbDateVariable("column A")),
            new WbSnakExpr(new WbPropConstant("P347", "reference URL", "url"), new WbStringVariable("column B"))));

    private Snak snak1 = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P87"),
            Datamodel.makeTimeValue(2018, (byte) 3, (byte) 28, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                    TimeValue.CM_GREGORIAN_PRO));
    private Snak snak2 = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P347"),
            Datamodel.makeStringValue("http://gnu.org/"));

    private String jsonRepresentation = "{\"snaks\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P87\","
            + "\"label\":\"retrieved\",\"datatype\":\"time\"},\"value\":{\"type\":\"wbdatevariable\","
            + "\"columnName\":\"column A\"}},{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P347\","
            + "\"label\":\"reference URL\",\"datatype\":\"url\"},\"value\":{\"type\":\"wbstringvariable\","
            + "\"columnName\":\"column B\"}}]}";

    @Test
    public void testEvaluate() {
        setRow("2018-03-28", "http://gnu.org/");
        evaluatesTo(Datamodel.makeReference(Arrays.asList(Datamodel.makeSnakGroup(Collections.singletonList(snak1)),
                Datamodel.makeSnakGroup(Collections.singletonList(snak2)))), expr);
    }

    @Test
    public void testEvaluateWithOneSkip() {
        setRow("invalid date", "http://gnu.org/");
        evaluatesTo(Datamodel.makeReference(Arrays.asList(Datamodel.makeSnakGroup(Collections.singletonList(snak2)))),
                expr);
    }

    @Test
    public void testNoValidSnak() {
        setRow("invalid date", "");
        isSkipped(expr);
    }

    @Test
    public void testSerialize()
            throws JsonProcessingException {
        JacksonSerializationTest.canonicalSerialization(WbReferenceExpr.class, expr, jsonRepresentation);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableList() {
        expr.getSnaks().clear();
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), false);
        columnModel.addColumn(0, new Column(0, "column B"), false);
        hasNoValidationError(expr, columnModel);
        hasValidationError("Null snak in reference", new WbReferenceExpr(Arrays.asList(
                null,
                new WbSnakExpr(new WbPropConstant("P347", "reference URL", "url"), new WbStringVariable("column B")))), columnModel);
    }
}
