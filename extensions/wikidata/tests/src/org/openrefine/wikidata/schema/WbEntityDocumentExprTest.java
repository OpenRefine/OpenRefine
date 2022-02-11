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

import java.util.Collections;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.openrefine.wikidata.updates.StatementEdit;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.openrefine.wikidata.updates.TermedStatementEntityEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

public class WbEntityDocumentExprTest extends WbExpressionTest<TermedStatementEntityEdit> {

    public WbEntityDocumentExpr expr;
    EntityIdValue subject = Datamodel.makeWikidataItemIdValue("Q23");
    MonolingualTextValue alias = Datamodel.makeMonolingualTextValue("my alias", "en");
    StatementEdit fullStatement;

    public String jsonRepresentation;

    public WbEntityDocumentExprTest() {
        WbStatementGroupExprTest sgt = new WbStatementGroupExprTest();
        WbNameDescExpr nde = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
                new WbMonolingualExpr(new WbLanguageConstant("en", "English"), new WbStringVariable("column D")));
        WbItemVariable subjectExpr = new WbItemVariable("column E");
        expr = new WbEntityDocumentExpr(subjectExpr, Collections.singletonList(nde), Collections.singletonList(sgt.expr));
        fullStatement = sgt.statementGroupUpdate.getStatementEdits().get(0);

        jsonRepresentation = "{\"subject\":{\"type\":\"wbitemvariable\",\"columnName\":\"column E\"},"
                + "\"nameDescs\":[{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"
                + "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},"
                + "\"value\":{\"type\":\"wbstringvariable\",\"columnName\":\"column D\"}}}" + "],\"statementGroups\":["
                + sgt.jsonRepresentation + "]}";
    }

    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", recon("Q23"));
        TermedStatementEntityEdit result = new TermedStatementEntityEditBuilder(subject).addAlias(alias).addStatement(fullStatement)
                .build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testSubjectSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", "not reconciled");
        isSkipped(expr);
    }

    @Test
    public void testStatementSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,invalid4.389", "my alias", recon("Q23"));
        TermedStatementEntityEdit result = new TermedStatementEntityEditBuilder(subject).addAlias(alias).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testAliasSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "", recon("Q23"));
        TermedStatementEntityEdit result = new TermedStatementEntityEditBuilder(subject).addStatement(fullStatement).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbEntityDocumentExpr.class, expr, jsonRepresentation);
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
