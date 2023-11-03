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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.openrefine.wikibase.updates.StatementEdit;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;
import com.google.refine.util.ParsingUtilities;

public class WbStatementExprTest extends WbExpressionTest<StatementEdit> {

    private WbReferenceExpr refExpr = new WbReferenceExpr(Arrays.asList(new WbSnakExpr(
            new WbPropConstant("P43", "imported from", "wikibase-item"), new WbItemVariable("column A"))));
    private WbSnakExpr qualifierExpr = new WbSnakExpr(new WbPropConstant("P897", "point in time", "time"),
            new WbDateVariable("column B"));
    private WbLocationVariable mainValueExpr = new WbLocationVariable("column C");
    public WbStatementExpr statementExpr = new WbStatementExpr(
            mainValueExpr,
            Collections.singletonList(qualifierExpr),
            Collections.singletonList(refExpr),
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);
    private WbSnakExpr constantQualifierExpr = new WbSnakExpr(new WbPropConstant("P897", "point in time", "time"),
            new WbDateConstant("2018-04-05"));
    public WbStatementExpr statementWithConstantExpr = new WbStatementExpr(
            mainValueExpr,
            Arrays.asList(qualifierExpr, constantQualifierExpr),
            Collections.singletonList(refExpr),
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);
    public WbStatementExpr statementDeleteExpr = new WbStatementExpr(
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            new PropertyOnlyStatementMerger(),
            StatementEditingMode.DELETE);

    public EntityIdValue subject = Datamodel.makeWikidataItemIdValue("Q23");
    private PropertyIdValue property = Datamodel.makeWikidataPropertyIdValue("P908");
    private Reference reference = Datamodel.makeReference(Collections.singletonList(Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P43"),
                    Datamodel.makeWikidataItemIdValue("Q3434"))))));

    private PropertyIdValue qualifierPid = Datamodel.makeWikidataPropertyIdValue("P897");

    private Snak qualifier = Datamodel.makeValueSnak(qualifierPid,
            Datamodel.makeTimeValue(2010, (byte) 7, (byte) 23, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                    TimeValue.CM_GREGORIAN_PRO));

    private Snak constantQualifier = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P897"),
            Datamodel.makeTimeValue(2018, (byte) 4, (byte) 5, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                    TimeValue.CM_GREGORIAN_PRO));
    private Snak mainsnak = Datamodel.makeValueSnak(property, Datamodel.makeGlobeCoordinatesValue(3.898, 4.389,
            WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH));
    private Claim fullClaim = Datamodel.makeClaim(subject, mainsnak,
            Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(qualifier))));
    public Statement fullStatement = Datamodel.makeStatement(fullClaim, Collections.singletonList(reference),
            StatementRank.NORMAL, "");

    public Statement fullStatementNoValue = Datamodel.makeStatement(Datamodel.makeClaim(subject, Datamodel.makeNoValueSnak(property),
            Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(qualifier)))), Collections.singletonList(reference),
            StatementRank.NORMAL, "");

    public Statement fullStatementSomeValue = Datamodel.makeStatement(Datamodel.makeClaim(subject, mainsnak,
            Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(Datamodel.makeSomeValueSnak(qualifierPid))))),
            Collections.singletonList(reference),
            StatementRank.NORMAL, "");

    public StatementEdit fullStatementUpdate = new StatementEdit(
            fullStatement,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);

    public StatementEdit fullStatementUpdateNoValue = new StatementEdit(
            fullStatementNoValue,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);

    public StatementEdit fullStatementUpdateSomeValue = new StatementEdit(
            fullStatementSomeValue,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);

    public Claim claimWithConstant = Datamodel.makeClaim(subject, mainsnak,
            Collections.singletonList(Datamodel.makeSnakGroup(Arrays.asList(qualifier, constantQualifier))));
    public Statement statementWithConstant = Datamodel.makeStatement(claimWithConstant, Collections.singletonList(reference),
            StatementRank.NORMAL, "");
    public StatementEdit statementUpdateWithConstant = new StatementEdit(
            statementWithConstant,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);

    class Wrapper implements WbExpression<StatementEdit> {

        public WbStatementExpr expr;

        public Wrapper(WbStatementExpr e) {
            expr = e;
        }

        @Override
        public StatementEdit evaluate(ExpressionContext ctxt)
                throws SkipSchemaExpressionException, QAWarningException {
            return expr.evaluate(ctxt, subject, property);
        }

        @Override
        public void validate(ValidationState validation) {
            expr.validate(validation);
        }
    }

    public String jsonRepresentation = "{"
            + "\"mergingStrategy\":{\"type\":\"qualifiers\",\"valueMatcher\":{\"type\":\"strict\"},\"pids\":[]},"
            + "\"mode\":\"add_or_merge\","
            + "\"value\":{\"type\":\"wblocationvariable\",\"columnName\":\"column C\"},"
            + "\"qualifiers\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P897\",\"label\":\"point in time\","
            + "\"datatype\":\"time\"},\"value\":{\"type\":\"wbdatevariable\",\"columnName\":\"column B\"}}],"
            + "\"references\":[{\"snaks\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P43\","
            + "\"label\":\"imported from\",\"datatype\":\"wikibase-item\"},\"value\":"
            + "{\"type\":\"wbitemvariable\",\"columnName\":\"column A\"}}]}]}";

    public String olderJsonRepresentation = "{\"value\":{\"type\":\"wblocationvariable\",\"columnName\":\"column C\"},"
            + "\"qualifiers\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P897\",\"label\":\"point in time\","
            + "\"datatype\":\"time\"},\"value\":{\"type\":\"wbdatevariable\",\"columnName\":\"column B\"}}],"
            + "\"references\":[{\"snaks\":[{\"prop\":{\"type\":\"wbpropconstant\",\"pid\":\"P43\","
            + "\"label\":\"imported from\",\"datatype\":\"wikibase-item\"},\"value\":"
            + "{\"type\":\"wbitemvariable\",\"columnName\":\"column A\"}}]}]}";

    public String jsonRepresentationDelete = "{"
            + "\"mergingStrategy\":{\"type\":\"property\"},"
            + "\"mode\":\"delete\","
            + "\"value\":null,"
            + "\"qualifiers\":[],"
            + "\"references\":[]}";

    @Test
    public void testCreation() {
        WbItemConstant q5 = new WbItemConstant("Q5", "human");
        WbStatementExpr empty = new WbStatementExpr(
                q5,
                Collections.emptyList(),
                Collections.emptyList(),
                StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);
        WbStatementExpr withNulls = new WbStatementExpr(
                q5, null, null, null, null);
        assertEquals(empty, withNulls);
    }

    @Test
    public void testAllowedNoMainValue() {
        WbStatementExpr expr = new WbStatementExpr(
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                new PropertyOnlyStatementMerger(),
                StatementEditingMode.DELETE);
        assertNull(expr.getMainsnak());
    }

    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(fullStatementUpdate, new Wrapper(statementExpr));
    }

    @Test
    public void testEvaluateWithConstant() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(statementUpdateWithConstant, new Wrapper(statementWithConstantExpr));
    }

    @Test
    public void testEvaluateWithoutReference() {
        setRow("not reconciled", "2010-07-23", "3.898,4.389");
        Statement statement = Datamodel.makeStatement(fullClaim, Collections.emptyList(), StatementRank.NORMAL, "");
        evaluatesTo(new StatementEdit(statement,
                StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE),
                new Wrapper(statementExpr));
    }

    @Test
    public void testEvaluateWithoutQualifier() {
        setRow(recon("Q3434"), "2010-invalid", "3.898,4.389");
        evaluatesTo(new StatementEdit(
                Datamodel.makeStatement(Datamodel.makeClaim(subject, mainsnak, Collections.emptyList()),
                        Collections.singletonList(reference), StatementRank.NORMAL, ""),
                StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE), new Wrapper(statementExpr));
    }

    @Test
    public void testEvaluateWithoutQualifierAndReference() {
        setRow("invalid", "2010-invalid", "3.898,4.389");
        evaluatesTo(new StatementEdit(Datamodel.makeStatement(
                Datamodel.makeClaim(subject, mainsnak, Collections.emptyList()),
                Collections.emptyList(), StatementRank.NORMAL, ""),
                StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE), new Wrapper(statementExpr));
    }

    @Test
    public void testSkip()
            throws JsonGenerationException, JsonMappingException, IOException {
        setRow(recon("Q3434"), "2010-07-23", "3.898,invalid");
        isSkipped(new Wrapper(statementExpr));
    }

    @Test
    public void testDeleteAllStatements() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(new StatementEdit(Datamodel.makeStatement(
                Datamodel.makeClaim(subject, Datamodel.makeNoValueSnak(property), Collections.emptyList()),
                Collections.emptyList(), StatementRank.NORMAL, ""),
                new PropertyOnlyStatementMerger(),
                StatementEditingMode.DELETE), new Wrapper(statementDeleteExpr));
    }

    @Test
    public void testEvaluateNoValueInMainSnak() {
        setRow(recon("Q3434"), "2010-07-23", WbVariableExpr.NO_VALUE_KEYWORD);
        evaluatesTo(fullStatementUpdateNoValue, new Wrapper(statementExpr));
    }

    @Test
    public void testEvaluateSomeValueInQualifier() {
        setRow(recon("Q3434"), WbVariableExpr.SOME_VALUE_KEYWORD, "3.898,4.389");
        evaluatesTo(fullStatementUpdateSomeValue, new Wrapper(statementExpr));
    }

    @Test
    public void testDeserializeOlderFormat() throws JsonMappingException, JsonProcessingException {
        // when no merging strategy or mode was provided
        WbStatementExpr deserialized = ParsingUtilities.mapper.readValue(olderJsonRepresentation, WbStatementExpr.class);
        JacksonSerializationTest.testSerialize(deserialized, jsonRepresentation);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbStatementExpr.class, statementExpr, jsonRepresentation);
    }

    @Test
    public void testSerializeDeleteStatement() {
        JacksonSerializationTest.canonicalSerialization(WbStatementExpr.class, statementDeleteExpr, jsonRepresentationDelete);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableQualifiersList() {
        statementExpr.getQualifiers().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableReferencesList() {
        statementExpr.getReferences().clear();
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), true);
        columnModel.addColumn(1, new Column(1, "column B"), true);
        columnModel.addColumn(2, new Column(2, "column C"), true);

        hasNoValidationError(new Wrapper(statementExpr), columnModel);
        hasNoValidationError(new Wrapper(statementDeleteExpr), columnModel);

        WbStatementExpr missingMainValue = new WbStatementExpr(
                null,
                Collections.singletonList(qualifierExpr),
                Collections.singletonList(refExpr),
                StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);
        hasValidationError("Missing main statement value", new Wrapper(missingMainValue), columnModel);

        WbStatementExpr nullQualifier = new WbStatementExpr(
                mainValueExpr,
                Collections.singletonList(null),
                Collections.singletonList(refExpr),
                StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);
        hasValidationError("Empty qualifier in statement", new Wrapper(nullQualifier), columnModel);
    }
}
