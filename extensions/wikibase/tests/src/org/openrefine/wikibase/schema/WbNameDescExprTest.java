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

import java.util.Collections;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbNameDescExprTest extends WbExpressionTest<MonolingualTextValue> {

    private ItemIdValue subject = Datamodel.makeWikidataItemIdValue("Q56");
    public WbNameDescExpr expr = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
            new WbMonolingualExpr(new WbLanguageConstant("en", "English"), new WbStringVariable("column A")));

    public String jsonRepresentation = "{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"
            + "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},\"value\":"
            + "{\"type\":\"wbstringvariable\",\"columnName\":\"column A\"}}}";

    @Test
    public void testContributeToLabel() throws QAWarningException {
        WbNameDescExpr labelExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.LABEL,
                TestingData.getTestMonolingualExpr("fr", "français", "le croissant magnifique"));
        ItemEditBuilder update = new ItemEditBuilder(subject);
        labelExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("le croissant magnifique", "fr")),
                update.build().getLabels());
    }

    @Test
    public void testContributeToDescription() throws QAWarningException {
        WbNameDescExpr descriptionExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.DESCRIPTION,
                TestingData.getTestMonolingualExpr("de", "Deutsch", "wunderschön"));
        ItemEditBuilder update = new ItemEditBuilder(subject);
        descriptionExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("wunderschön", "de")),
                update.build().getDescriptions());
    }

    @Test
    public void testContributeToAlias() throws QAWarningException {
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS,
                TestingData.getTestMonolingualExpr("en", "English", "snack"));
        ItemEditBuilder update = new ItemEditBuilder(subject);
        aliasExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("snack", "en")),
                update.build().getAliases());
    }

    @Test
    public void testSkipped() throws QAWarningException {
        ItemEditBuilder update = new ItemEditBuilder(subject);
        setRow("");
        expr.contributeTo(update, ctxt);
        assertEquals(new ItemEditBuilder(subject).build(), update.build());
    }

    @Test
    public void testGetters() {
        WbMonolingualExpr monolingualExpr = TestingData.getTestMonolingualExpr("en", "English", "not sure what");
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescType.ALIAS, monolingualExpr);
        assertEquals(aliasExpr.getType(), WbNameDescExpr.NameDescType.ALIAS);
        assertEquals(aliasExpr.getValue(), monolingualExpr);
        assertEquals(aliasExpr.getStaticLanguage(), "English");
    }

    @Test
    public void testSerialization() {
        JacksonSerializationTest.canonicalSerialization(WbNameDescExpr.class, expr, jsonRepresentation);
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), true);
        ValidationState validationState = new ValidationState(columnModel);
        expr.validate(validationState);
        Assert.assertTrue(validationState.getValidationErrors().isEmpty());
    }
}
