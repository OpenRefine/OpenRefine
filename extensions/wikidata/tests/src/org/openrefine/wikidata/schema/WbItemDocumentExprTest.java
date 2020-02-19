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
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public class WbItemDocumentExprTest extends WbExpressionTest<ItemUpdate> {

    public WbItemDocumentExpr expr;
    ItemIdValue subject = Datamodel.makeWikidataItemIdValue("Q23");
    MonolingualTextValue alias = Datamodel.makeMonolingualTextValue("my alias", "en");
    Statement fullStatement;

    public String jsonRepresentation;

    public WbItemDocumentExprTest() {
        WbStatementGroupExprTest sgt = new WbStatementGroupExprTest();
        WbNameDescExpr nde = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
                new WbMonolingualExpr(new WbLanguageConstant("en", "English"), new WbStringVariable("column D")));
        WbItemVariable subjectExpr = new WbItemVariable("column E");
        expr = new WbItemDocumentExpr(subjectExpr, Collections.singletonList(nde), Collections.singletonList(sgt.expr));
        fullStatement = sgt.statementGroup.getStatements().get(0);

        jsonRepresentation = "{\"subject\":{\"type\":\"wbitemvariable\",\"columnName\":\"column E\"},"
                + "\"nameDescs\":[{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"
                + "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},"
                + "\"value\":{\"type\":\"wbstringvariable\",\"columnName\":\"column D\"}}}" + "],\"statementGroups\":["
                + sgt.jsonRepresentation + "]}";
    }

    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "my alias", recon("Q23"));
        ItemUpdate result = new ItemUpdateBuilder(subject).addAlias(alias).addStatement(fullStatement).build();
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
        ItemUpdate result = new ItemUpdateBuilder(subject).addAlias(alias).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testAliasSkipped() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389", "", recon("Q23"));
        ItemUpdate result = new ItemUpdateBuilder(subject).addStatement(fullStatement).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbItemDocumentExpr.class, expr, jsonRepresentation);
    }
}
