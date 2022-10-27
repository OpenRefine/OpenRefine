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

import java.util.Collections;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarning.Severity;
import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbItemEditExprTest extends WbExpressionTest<ItemEdit> {

    public WbItemEditExpr expr;
    EntityIdValue subject = Datamodel.makeWikidataItemIdValue("Q23");
    MonolingualTextValue alias = Datamodel.makeMonolingualTextValue("my alias", "en");
    StatementEdit fullStatement;

    public String jsonRepresentation;

    public WbItemEditExprTest() {
        WbStatementGroupExprTest sgt = new WbStatementGroupExprTest();
        WbNameDescExpr nde = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
                new WbMonolingualExpr(new WbLanguageConstant("en", "English"), new WbStringVariable("column D")));
        WbEntityVariable subjectExpr = new WbEntityVariable("column E");
        expr = new WbItemEditExpr(subjectExpr, Collections.singletonList(nde), Collections.singletonList(sgt.expr));
        fullStatement = sgt.statementGroupUpdate.getStatementEdits().get(0);

        jsonRepresentation = "{\"type\":\"wbitemeditexpr\","
                + "\"subject\":{\"type\":\"wbentityvariable\",\"columnName\":\"column E\"},"
                + "\"nameDescs\":[{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"
                + "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},"
                + "\"value\":{\"type\":\"wbstringvariable\",\"columnName\":\"column D\"}}}" + "],\"statementGroups\":["
                + sgt.jsonRepresentation + "]}";
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), false);
        columnModel.addColumn(0, new Column(0, "column B"), false);
        columnModel.addColumn(0, new Column(0, "column C"), false);
        columnModel.addColumn(0, new Column(0, "column D"), false);
        columnModel.addColumn(0, new Column(0, "column E"), false);

        hasNoValidationError(expr, columnModel);
        hasValidationError("No subject item id provided", new WbItemEditExpr(null, null, null), columnModel);
    }

    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", recon("Q23"));
        ItemEdit result = new ItemEditBuilder(subject).addAlias(alias).addStatement(fullStatement)
                .build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testEvaluateInvalidSubjectType() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", recon("M23"));
        QAWarning warning = new QAWarning(WbItemEditExpr.INVALID_SUBJECT_WARNING_TYPE, "", Severity.CRITICAL, 1);
        warning.setProperty("example", "M23");
        evaluatesToWarning(warning, expr);
    }

    @Test
    public void testSubjectSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", "not reconciled");
        isSkipped(expr);
    }

    @Test
    public void testStatementSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,invalid4.389", "my alias", recon("Q23"));
        ItemEdit result = new ItemEditBuilder(subject).addAlias(alias).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testAliasSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "", recon("Q23"));
        ItemEdit result = new ItemEditBuilder(subject).addStatement(fullStatement).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbItemEditExpr.class, expr, jsonRepresentation);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableNameDescsList() {
        expr.getNameDescs().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableStatementGroupsList() {
        expr.getStatementGroups().clear();
    }
}
