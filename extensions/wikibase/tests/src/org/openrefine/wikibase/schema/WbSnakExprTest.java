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
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbSnakExprTest extends WbExpressionTest<Snak> {

    private PropertyIdValue propStringId = Datamodel.makeWikidataPropertyIdValue("P89");
    private WbPropConstant propStringExpr = new WbPropConstant("P89", "prop label", "string");
    private WbSnakExpr expr = new WbSnakExpr(propStringExpr, new WbStringVariable("column A"));

    public String jsonRepresentation = "{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P89\","
            + "\"label\":\"prop label\",\"datatype\":\"string\"},\"value\":"
            + "{\"type\":\"wbstringvariable\",\"columnName\":\"column A\"}}";

    @Test
    public void testEvaluate() {
        setRow("cinema");
        evaluatesTo(Datamodel.makeValueSnak(propStringId, Datamodel.makeStringValue("cinema")), expr);
    }

    @Test
    public void testSerialize()
            throws JsonProcessingException {
        JacksonSerializationTest.canonicalSerialization(WbSnakExpr.class, expr, jsonRepresentation);
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), true);

        hasNoValidationError(expr, columnModel);
        WbSnakExpr missingValue = new WbSnakExpr(propStringExpr, null);
        hasValidationError("Missing value", missingValue, columnModel);
    }

    // TODO check that the datatype of the property matches that of the datavalue
    // (important when we introduce property variables)
}
