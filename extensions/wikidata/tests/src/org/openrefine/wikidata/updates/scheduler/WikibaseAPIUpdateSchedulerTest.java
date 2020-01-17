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
import java.util.List;

import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;

public class WikibaseAPIUpdateSchedulerTest extends UpdateSchedulerTest {

    @Test
    public void testOrderPreserved()
            throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoB).build();
        ItemUpdate updateB = new ItemUpdateBuilder(existingIdB).addStatement(sBtoA).build();
        List<ItemUpdate> scheduled = schedule(updateA, updateB);
        assertEquals(Arrays.asList(updateA, updateB), scheduled);
    }

    @Test
    public void testUpdateIsNotSplit()
            throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoNewA).addStatement(sAtoNewB).build();
        ItemUpdate newUpdateA = new ItemUpdateBuilder(newIdA).build();
        ItemUpdate newUpdateB = new ItemUpdateBuilder(newIdB).build();
        List<ItemUpdate> scheduled = schedule(updateA);
        assertSetEquals(Arrays.asList(newUpdateA, newUpdateB, updateA), scheduled);
    }

    @Test
    public void testMixedUpdate()
            throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoNewA).addStatement(sAtoNewB)
                .addStatement(sAtoB).build();
        ItemUpdate newUpdateA = new ItemUpdateBuilder(newIdA).addStatement(sNewAtoB).build();
        ItemUpdate newUpdateB = new ItemUpdateBuilder(newIdB).build();
        List<ItemUpdate> scheduled = schedule(updateA, newUpdateA);
        assertEquals(Arrays.asList(newUpdateA, newUpdateB, updateA), scheduled);
    }

    @Override
    public UpdateScheduler getScheduler() {
        return new WikibaseAPIUpdateScheduler();
    }

}
