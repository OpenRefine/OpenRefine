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

package org.openrefine.wikibase.updates.scheduler;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.testng.annotations.Test;

public class WikibaseAPIUpdateSchedulerTest extends UpdateSchedulerTest {

    @Test
    public void testOrderPreserved()
            throws ImpossibleSchedulingException {
        ItemEdit updateA = new ItemEditBuilder(existingIdA).addStatement(sAtoB).build();
        ItemEdit updateB = new ItemEditBuilder(existingIdB).addStatement(sBtoA).build();
        List<EntityEdit> scheduled = schedule(updateA, updateB);
        assertEquals(scheduled, Arrays.asList(updateA, updateB));
    }

    @Test
    public void testUpdateIsNotSplit()
            throws ImpossibleSchedulingException {
        ItemEdit updateA = new ItemEditBuilder(existingIdA).addStatement(sAtoNewA)
                .addStatement(sAtoNewB).build();
        ItemEdit newUpdateA = new ItemEditBuilder(newIdA).build();
        ItemEdit newUpdateB = new ItemEditBuilder(newIdB).build();
        List<EntityEdit> scheduled = schedule(updateA);
        assertSetEquals(scheduled, Arrays.asList(newUpdateA, newUpdateB, updateA));
    }

    @Test
    public void testMixedUpdate()
            throws ImpossibleSchedulingException {
        EntityEdit updateA = new ItemEditBuilder(existingIdA).addStatement(sAtoNewA)
                .addStatement(sAtoNewB)
                .addStatement(sAtoB).build();
        EntityEdit newUpdateA = new ItemEditBuilder(newIdA).addStatement(sNewAtoB).build();
        EntityEdit newUpdateB = new ItemEditBuilder(newIdB).build();
        List<EntityEdit> scheduled = schedule(updateA, newUpdateA);
        assertEquals(scheduled, Arrays.asList(newUpdateA, newUpdateB, updateA));
    }

    @Test
    public void testMediaInfoReferringToNewItem() throws ImpossibleSchedulingException {
        EntityEdit updateMediaInfo = new MediaInfoEditBuilder(existingMediaInfoId)
                .addStatement(TestingData.generateStatementAddition(existingMediaInfoId, newIdA))
                .build();
        EntityEdit newUpdateA = new ItemEditBuilder(newIdA).build();
        List<EntityEdit> scheduled = schedule(updateMediaInfo);
        assertEquals(scheduled, Arrays.asList(newUpdateA, updateMediaInfo));
    }

    @Override
    public UpdateScheduler getScheduler() {
        return new WikibaseAPIUpdateScheduler();
    }

}
