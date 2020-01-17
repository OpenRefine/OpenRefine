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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class WbStatementExprTest extends WbExpressionTest<Statement> {

    private WbReferenceExpr refExpr = new WbReferenceExpr(Arrays.asList(new WbSnakExpr(
            new WbPropConstant("P43", "imported from", "wikibase-item"), new WbItemVariable("column A"))));
    private WbSnakExpr qualifierExpr = new WbSnakExpr(new WbPropConstant("P897", "point in time", "time"),
            new WbDateVariable("column B"));
    private WbLocationVariable mainValueExpr = new WbLocationVariable("column C");
    public WbStatementExpr statementExpr = new WbStatementExpr(mainValueExpr, Collections.singletonList(qualifierExpr),
            Collections.singletonList(refExpr));
    private WbSnakExpr constantQualifierExpr = new WbSnakExpr(new WbPropConstant("P897", "point in time", "time"),
            new WbDateConstant("2018-04-05"));
    public WbStatementExpr statementWithConstantExpr = new WbStatementExpr(mainValueExpr,
            Arrays.asList(qualifierExpr, constantQualifierExpr),
            Collections.singletonList(refExpr));

    public ItemIdValue subject = Datamodel.makeWikidataItemIdValue("Q23");
    private PropertyIdValue property = Datamodel.makeWikidataPropertyIdValue("P908");
    private Reference reference = Datamodel.makeReference(Collections.singletonList(Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P43"),
                    Datamodel.makeWikidataItemIdValue("Q3434"))))));
    private Snak qualifier = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P897"),
            Datamodel.makeTimeValue(2010, (byte) 7, (byte) 23, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO));
    private Snak constantQualifier = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P897"),
            Datamodel.makeTimeValue(2018, (byte) 4, (byte) 5, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO));
    private Snak mainsnak = Datamodel.makeValueSnak(property, Datamodel.makeGlobeCoordinatesValue(3.898, 4.389,
            WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH));
    private Claim fullClaim = Datamodel.makeClaim(subject, mainsnak,
            Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(qualifier))));
    public Statement fullStatement = Datamodel.makeStatement(fullClaim, Collections.singletonList(reference),
            StatementRank.NORMAL, "");
    public Claim claimWithConstant = Datamodel.makeClaim(subject, mainsnak,
            Collections.singletonList(Datamodel.makeSnakGroup(Arrays.asList(qualifier, constantQualifier))));
    public Statement statementWithConstant = Datamodel.makeStatement(claimWithConstant, Collections.singletonList(reference),
            StatementRank.NORMAL, "");

    class Wrapper implements WbExpression<Statement> {

        public WbStatementExpr expr;

        public Wrapper(WbStatementExpr e) {
            expr = e;
        }

        @Override
        public Statement evaluate(ExpressionContext ctxt)
                throws SkipSchemaExpressionException {
            return expr.evaluate(ctxt, subject, property);
        }
    }

    public String jsonRepresentation = "{\"value\":{\"type\":\"wblocationvariable\",\"columnName\":\"column C\"},"
            + "\"qualifiers\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P897\",\"label\":\"point in time\","
            + "\"datatype\":\"time\"},\"value\":{\"type\":\"wbdatevariable\",\"columnName\":\"column B\"}}],"
            + "\"references\":[{\"snaks\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P43\","
            + "\"label\":\"imported from\",\"datatype\":\"wikibase-item\"},\"value\":"
            + "{\"type\":\"wbitemvariable\",\"columnName\":\"column A\"}}]}]}";

    @Test
    public void testCreation() {
        WbItemConstant q5 = new WbItemConstant("Q5", "human");
        WbStatementExpr empty = new WbStatementExpr(q5, Collections.emptyList(), Collections.emptyList());
        WbStatementExpr withNulls = new WbStatementExpr(q5, null, null);
        assertEquals(empty, withNulls);
    }

    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(fullStatement, new Wrapper(statementExpr));
    }
    
    @Test
    public void testEvaluateWithConstant() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(statementWithConstant, new Wrapper(statementWithConstantExpr));
    }

    @Test
    public void testEvaluateWithoutReference() {
        setRow("not reconciled", "2010-07-23", "3.898,4.389");
        evaluatesTo(Datamodel.makeStatement(fullClaim, Collections.emptyList(), StatementRank.NORMAL, ""),
                new Wrapper(statementExpr));
    }

    @Test
    public void testEvaluateWithoutQualifier() {
        setRow(recon("Q3434"), "2010-invalid", "3.898,4.389");
        evaluatesTo(Datamodel.makeStatement(Datamodel.makeClaim(subject, mainsnak, Collections.emptyList()),
                Collections.singletonList(reference), StatementRank.NORMAL, ""), new Wrapper(statementExpr));
    }

    @Test
    public void testEvaluateWithoutQualifierAndReference() {
        setRow("invalid", "2010-invalid", "3.898,4.389");
        evaluatesTo(Datamodel.makeStatement(Datamodel.makeClaim(subject, mainsnak, Collections.emptyList()),
                Collections.emptyList(), StatementRank.NORMAL, ""), new Wrapper(statementExpr));
    }

    @Test
    public void testSkip()
            throws JsonGenerationException, JsonMappingException, IOException {
        setRow(recon("Q3434"), "2010-07-23", "3.898,invalid");
        isSkipped(new Wrapper(statementExpr));
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbStatementExpr.class, statementExpr, jsonRepresentation);
    }
}
