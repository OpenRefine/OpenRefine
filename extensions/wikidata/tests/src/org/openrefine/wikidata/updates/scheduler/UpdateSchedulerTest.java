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

package org.openrefine.wikidata.updates.scheduler;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.StatementEdit;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.openrefine.wikidata.updates.TermedStatementEntityEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public abstract class UpdateSchedulerTest {

    protected ItemIdValue existingIdA = Datamodel.makeWikidataItemIdValue("Q43");
    protected ItemIdValue existingIdB = Datamodel.makeWikidataItemIdValue("Q538");
    protected ItemIdValue newIdA = TestingData.newIdA;
    protected ItemIdValue newIdB = TestingData.newIdB;

    protected StatementEdit sAtoB = TestingData.generateStatementAddition(existingIdA, existingIdB);
    protected StatementEdit sBtoA = TestingData.generateStatementAddition(existingIdB, existingIdA);
    protected StatementEdit sAtoNewA = TestingData.generateStatementAddition(existingIdA, newIdA);
    protected StatementEdit sAtoNewB = TestingData.generateStatementAddition(existingIdA, newIdB);
    protected StatementEdit sNewAtoB = TestingData.generateStatementAddition(newIdA, existingIdB);
    protected StatementEdit sNewAtoNewB = TestingData.generateStatementAddition(newIdA, newIdB);
    protected StatementEdit sNewAtoNewA = TestingData.generateStatementAddition(newIdA, newIdA);

    public abstract UpdateScheduler getScheduler();

    protected List<TermedStatementEntityEdit> schedule(TermedStatementEntityEdit... itemUpdates)
            throws ImpossibleSchedulingException {
        return getScheduler().schedule(Arrays.asList(itemUpdates));
    }

    protected static void assertSetEquals(List<TermedStatementEntityEdit> expected, List<TermedStatementEntityEdit> actual) {
        assertEquals(expected.stream().collect(Collectors.toSet()), actual.stream().collect(Collectors.toSet()));
    }

    @Test
    public void testNewItemNotMentioned()
            throws ImpossibleSchedulingException {
        TermedStatementEntityEdit updateA = new TermedStatementEntityEditBuilder(existingIdA).addStatement(sAtoNewA).build();
        List<TermedStatementEntityEdit> scheduled = schedule(updateA);
        TermedStatementEntityEdit newUpdate = new TermedStatementEntityEditBuilder(newIdA).build();
        assertEquals(Arrays.asList(newUpdate, updateA), scheduled);
    }

    @Test
    public void testNewItemMentioned()
            throws ImpossibleSchedulingException {
        TermedStatementEntityEdit updateA = new TermedStatementEntityEditBuilder(existingIdA).addStatement(sAtoNewA).build();
        TermedStatementEntityEdit newUpdate = new TermedStatementEntityEditBuilder(newIdA).addStatement(sNewAtoB).build();
        List<TermedStatementEntityEdit> scheduled = schedule(updateA, newUpdate);
        assertEquals(Arrays.asList(newUpdate, updateA), scheduled);
    }

    @Test
    public void testMerge()
            throws ImpossibleSchedulingException {
        TermedStatementEntityEdit update1 = new TermedStatementEntityEditBuilder(existingIdA).addStatement(sAtoB).build();
        TermedStatementEntityEdit update2 = new TermedStatementEntityEditBuilder(existingIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"), true).addStatement(sAtoB).build();
        TermedStatementEntityEdit merged = update1.merge(update2);
        assertEquals(Collections.singletonList(merged), schedule(update1, update2));
    }

    @Test
    public void testMergeNew()
            throws ImpossibleSchedulingException {
        TermedStatementEntityEdit update1 = new TermedStatementEntityEditBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"), true)
                .addStatement(sNewAtoB).build();
        TermedStatementEntityEdit update2 = new TermedStatementEntityEditBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"), true)
                .build();
        TermedStatementEntityEdit merged = update1.merge(update2);
        assertEquals(Collections.singletonList(merged), schedule(update1, update2));
    }
}
