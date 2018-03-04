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
import java.util.List;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public class RestrictedPositionScrutinizerTest extends SnakScrutinizerTest {

    private ItemIdValue qid = TestingData.existingId;

    @Override
    public EditScrutinizer getScrutinizer() {
        return new RestrictedPositionScrutinizer();
    }

    @Test
    public void testTriggerMainSnak() {
        scrutinize(TestingData.generateStatement(qid, MockConstraintFetcher.qualifierPid, qid));
        assertWarningsRaised("property-restricted-to-qualifier-found-in-mainsnak");
    }

    @Test
    public void testNoProblem() {
        scrutinize(TestingData.generateStatement(qid, MockConstraintFetcher.mainSnakPid, qid));
        assertNoWarningRaised();
    }

    @Test
    public void testNotRestricted() {
        scrutinize(TestingData.generateStatement(qid, Datamodel.makeWikidataPropertyIdValue("P3748"), qid));
        assertNoWarningRaised();
    }

    @Test
    public void testTriggerReference() {
        Snak snak = Datamodel.makeValueSnak(MockConstraintFetcher.mainSnakPid, qid);
        List<SnakGroup> snakGroups = Collections
                .singletonList(Datamodel.makeSnakGroup(Collections.singletonList(snak)));
        Statement statement = Datamodel.makeStatement(
                TestingData.generateStatement(qid, MockConstraintFetcher.mainSnakPid, qid).getClaim(),
                Collections.singletonList(Datamodel.makeReference(snakGroups)), StatementRank.NORMAL, "");
        scrutinize(statement);
        assertWarningsRaised("property-restricted-to-mainsnak-found-in-reference");
    }

}
