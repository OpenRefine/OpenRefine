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

import java.util.Collections;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.TermedStatementEntityEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

public class WbNameDescExprTest extends WbExpressionTest<MonolingualTextValue> {

    private ItemIdValue subject = Datamodel.makeWikidataItemIdValue("Q56");
    public WbNameDescExpr expr = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
            new WbMonolingualExpr(new WbLanguageConstant("en", "English"), new WbStringVariable("column A")));

    public String jsonRepresentation = "{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"
            + "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},\"value\":"
            + "{\"type\":\"wbstringvariable\",\"columnName\":\"column A\"}}}";

    @Test
    public void testContributeToLabel() {
        WbNameDescExpr labelExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.LABEL,
                TestingData.getTestMonolingualExpr("fr", "français", "le croissant magnifique"));
        TermedStatementEntityEditBuilder update = new TermedStatementEntityEditBuilder(subject);
        labelExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("le croissant magnifique", "fr")),
                update.build().getLabels());
    }

    @Test
    public void testContributeToDescription() {
        WbNameDescExpr descriptionExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.DESCRIPTION,
                TestingData.getTestMonolingualExpr("de", "Deutsch", "wunderschön"));
        TermedStatementEntityEditBuilder update = new TermedStatementEntityEditBuilder(subject);
        descriptionExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("wunderschön", "de")),
                update.build().getDescriptions());
    }

    @Test
    public void testContributeToAlias() {
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
                TestingData.getTestMonolingualExpr("en", "English", "snack"));
        TermedStatementEntityEditBuilder update = new TermedStatementEntityEditBuilder(subject);
        aliasExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("snack", "en")),
                update.build().getAliases());
    }

    @Test
    public void testSkipped() {
        TermedStatementEntityEditBuilder update = new TermedStatementEntityEditBuilder(subject);
        setRow("");
        expr.contributeTo(update, ctxt);
        assertEquals(new TermedStatementEntityEditBuilder(subject).build(), update.build());
    }

    @Test
    public void testGetters() {
        WbMonolingualExpr monolingualExpr = TestingData.getTestMonolingualExpr("en", "English", "not sure what");
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS, monolingualExpr);
        assertEquals(WbNameDescExpr.NameDescType.ALIAS, aliasExpr.getType());
        assertEquals(monolingualExpr, aliasExpr.getValue());
    }

    @Test
    public void testSerialization() {
        JacksonSerializationTest.canonicalSerialization(WbNameDescExpr.class, expr, jsonRepresentation);
    }
}
