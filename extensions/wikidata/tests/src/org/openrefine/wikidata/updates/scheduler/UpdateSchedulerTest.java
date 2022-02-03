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
import org.openrefine.wikidata.updates.TermedStatementEntityUpdate;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class UpdateSchedulerTest {

    protected ItemIdValue existingIdA = Datamodel.makeWikidataItemIdValue("Q43");
    protected ItemIdValue existingIdB = Datamodel.makeWikidataItemIdValue("Q538");
    protected ItemIdValue newIdA = TestingData.newIdA;
    protected ItemIdValue newIdB = TestingData.newIdB;

    protected Statement sAtoB = TestingData.generateStatement(existingIdA, existingIdB);
    protected Statement sBtoA = TestingData.generateStatement(existingIdB, existingIdA);
    protected Statement sAtoNewA = TestingData.generateStatement(existingIdA, newIdA);
    protected Statement sAtoNewB = TestingData.generateStatement(existingIdA, newIdB);
    protected Statement sNewAtoB = TestingData.generateStatement(newIdA, existingIdB);
    protected Statement sNewAtoNewB = TestingData.generateStatement(newIdA, newIdB);
    protected Statement sNewAtoNewA = TestingData.generateStatement(newIdA, newIdA);

    public abstract UpdateScheduler getScheduler();

    protected List<TermedStatementEntityUpdate> schedule(TermedStatementEntityUpdate... itemUpdates)
            throws ImpossibleSchedulingException {
        return getScheduler().schedule(Arrays.asList(itemUpdates));
    }

    protected static void assertSetEquals(List<TermedStatementEntityUpdate> expected, List<TermedStatementEntityUpdate> actual) {
        assertEquals(expected.stream().collect(Collectors.toSet()), actual.stream().collect(Collectors.toSet()));
    }

    @Test
    public void testNewItemNotMentioned()
            throws ImpossibleSchedulingException {
        TermedStatementEntityUpdate updateA = new TermedStatementEntityUpdateBuilder(existingIdA).addStatement(sAtoNewA).build();
        List<TermedStatementEntityUpdate> scheduled = schedule(updateA);
        TermedStatementEntityUpdate newUpdate = new TermedStatementEntityUpdateBuilder(newIdA).build();
        assertEquals(Arrays.asList(newUpdate, updateA), scheduled);
    }

    @Test
    public void testNewItemMentioned()
            throws ImpossibleSchedulingException {
        TermedStatementEntityUpdate updateA = new TermedStatementEntityUpdateBuilder(existingIdA).addStatement(sAtoNewA).build();
        TermedStatementEntityUpdate newUpdate = new TermedStatementEntityUpdateBuilder(newIdA).addStatement(sNewAtoB).build();
        List<TermedStatementEntityUpdate> scheduled = schedule(updateA, newUpdate);
        assertEquals(Arrays.asList(newUpdate, updateA), scheduled);
    }

    @Test
    public void testMerge()
            throws ImpossibleSchedulingException {
        TermedStatementEntityUpdate update1 = new TermedStatementEntityUpdateBuilder(existingIdA).addStatement(sAtoB).build();
        TermedStatementEntityUpdate update2 = new TermedStatementEntityUpdateBuilder(existingIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"), true).addStatement(sAtoB).build();
        TermedStatementEntityUpdate merged = update1.merge(update2);
        assertEquals(Collections.singletonList(merged), schedule(update1, update2));
    }

    @Test
    public void testMergeNew()
            throws ImpossibleSchedulingException {
        TermedStatementEntityUpdate update1 = new TermedStatementEntityUpdateBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"), true)
                .addStatement(sNewAtoB).build();
        TermedStatementEntityUpdate update2 = new TermedStatementEntityUpdateBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"), true)
                .build();
        TermedStatementEntityUpdate merged = update1.merge(update2);
        assertEquals(Collections.singletonList(merged), schedule(update1, update2));
    }
}
