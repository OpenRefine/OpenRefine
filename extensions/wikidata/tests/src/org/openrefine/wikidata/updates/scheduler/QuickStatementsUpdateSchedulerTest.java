package org.openrefine.wikidata.updates.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;


public class QuickStatementsUpdateSchedulerTest extends UpdateSchedulerTest {
    
    @Test
    public void testNoNewItem() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoB).build();
        ItemUpdate updateB = new ItemUpdateBuilder(existingIdB).addStatement(sBtoA).build();
        List<ItemUpdate> scheduled = schedule(updateA, updateB);
        assertEquals(Arrays.asList(updateA,updateB), scheduled);
    }
    
    @Test
    public void testSplitUpdate() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA)
                .addStatement(sAtoNewA)
                .addStatement(sAtoNewB)
                .build();
        ItemUpdate newUpdateA = new ItemUpdateBuilder(newIdA).build();
        ItemUpdate newUpdateB = new ItemUpdateBuilder(newIdB).build();
        ItemUpdate splitUpdateA = new ItemUpdateBuilder(existingIdA)
                .addStatement(sAtoNewA)
                .build();
        ItemUpdate splitUpdateB = new ItemUpdateBuilder(existingIdA)
                .addStatement(sAtoNewB)
                .build();
        List<ItemUpdate> scheduled = schedule(updateA);
        assertSetEquals(Arrays.asList(newUpdateA, splitUpdateA, newUpdateB, splitUpdateB), scheduled);
    }
    
    @Test(expectedExceptions=ImpossibleSchedulingException.class)
    public void testImpossibleForQS() throws ImpossibleSchedulingException {
        ItemUpdate update = new ItemUpdateBuilder(newIdA).addStatement(sNewAtoNewB).build();
        schedule(update);
    }
    
    @Test
    public void testSelfEditOnNewITem() throws ImpossibleSchedulingException {
        ItemUpdate update = new ItemUpdateBuilder(newIdA).addStatement(sNewAtoNewA).build();
        assertEquals(Arrays.asList(update), schedule(update));
    }

    @Override
    public UpdateScheduler getScheduler() {
        return new QuickStatementsUpdateScheduler();
    }
}
