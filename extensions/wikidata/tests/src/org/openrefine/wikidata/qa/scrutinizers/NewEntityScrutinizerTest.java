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

package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Collections;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.openrefine.wikidata.updates.ItemEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public class NewEntityScrutinizerTest extends ScrutinizerTest {

    private Claim claim = Datamodel.makeClaim(TestingData.newIdA,
            Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P31"), TestingData.existingId),
            Collections.emptyList());
    private Statement p31Statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new NewEntityScrutinizer();
    }

    @Test
    public void testTrigger() {
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA).build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.noDescType, NewEntityScrutinizer.noLabelType,
                NewEntityScrutinizer.noTypeType, NewEntityScrutinizer.newItemType);
    }

    @Test
    public void testEmptyItem() {
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.existingId).build();
        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testGoodNewItem() {

        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("interesting item", "en"), true)
                .addStatement(add(p31Statement))
                .build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newItemType);
    }

    @Test
    public void testDeletedStatements() {
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("interesting item", "en"), true)
                .addStatement(add(p31Statement))
                .addStatement(delete(TestingData.generateStatement(TestingData.newIdA, TestingData.matchedId)))
                .build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newItemType, NewEntityScrutinizer.deletedStatementsType);
    }

}
