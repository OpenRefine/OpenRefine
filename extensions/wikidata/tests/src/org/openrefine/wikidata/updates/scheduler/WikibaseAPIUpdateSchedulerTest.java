package org.openrefine.wikidata.updates.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;


public class WikibaseAPIUpdateSchedulerTest extends UpdateSchedulerTest {

    @Test
    public void testOrderPreserved() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoB).build();
        ItemUpdate updateB = new ItemUpdateBuilder(existingIdB).addStatement(sBtoA).build();
        List<ItemUpdate> scheduled = schedule(updateA, updateB);
        assertEquals(Arrays.asList(updateA,updateB), scheduled);
    }
    
    @Test
    public void testUpdateIsNotSplit() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA)
                .addStatement(sAtoNewA)
                .addStatement(sAtoNewB)
                .build();
        ItemUpdate newUpdateA = new ItemUpdateBuilder(newIdA).build();
        ItemUpdate newUpdateB = new ItemUpdateBuilder(newIdB).build();
        List<ItemUpdate> scheduled = schedule(updateA);
        assertSetEquals(Arrays.asList(newUpdateA, newUpdateB, updateA), scheduled);
    }
    
    @Test
    public void testMixedUpdate() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA)
                .addStatement(sAtoNewA)
                .addStatement(sAtoNewB)
                .addStatement(sAtoB)
                .build();
        ItemUpdate newUpdateA = new ItemUpdateBuilder(newIdA)
                .addStatement(sNewAtoB)
                .build();
        ItemUpdate newUpdateB = new ItemUpdateBuilder(newIdB)
                .build();
        List<ItemUpdate> scheduled = schedule(updateA, newUpdateA);
        assertEquals(Arrays.asList(newUpdateA, newUpdateB, updateA), scheduled);
    }

    @Override
    public UpdateScheduler getScheduler() {
        return new WikibaseAPIUpdateScheduler();
    }

}
