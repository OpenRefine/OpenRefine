package org.openrefine.wikidata.updates.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public abstract class UpdateSchedulerTest {

    protected ItemIdValue existingIdA = Datamodel.makeWikidataItemIdValue("Q43");
    protected ItemIdValue existingIdB = Datamodel.makeWikidataItemIdValue("Q538");
    protected ItemIdValue newIdA = TestingDataGenerator.makeNewItemIdValue(1234L, "new item A");
    protected ItemIdValue newIdB = TestingDataGenerator.makeNewItemIdValue(5678L, "new item B");
    
    protected static PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P38");
    
    protected Statement sAtoB = generateStatement(existingIdA, existingIdB);
    protected Statement sBtoA = generateStatement(existingIdB, existingIdA);
    protected Statement sAtoNewA = generateStatement(existingIdA, newIdA);
    protected Statement sAtoNewB = generateStatement(existingIdA, newIdB);
    protected Statement sNewAtoB = generateStatement(newIdA, existingIdB);
    protected Statement sNewAtoNewB = generateStatement(newIdA, newIdB);
    
    public static Statement generateStatement(ItemIdValue from, ItemIdValue to) {
        Claim claim = Datamodel.makeClaim(from, Datamodel.makeValueSnak(pid, to), Collections.emptyList());
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }
    
    public abstract UpdateScheduler getScheduler();
    
    protected List<ItemUpdate> schedule(ItemUpdate... itemUpdates) throws ImpossibleSchedulingException {
        return getScheduler().schedule(Arrays.asList(itemUpdates));
    }
    
    protected static void assertSetEquals(List<ItemUpdate> expected, List<ItemUpdate> actual) {
        assertEquals(expected.stream().collect(Collectors.toSet()),
                actual.stream().collect(Collectors.toSet()));
    }
    
    @Test
    public void testNewItemNotMentioned() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoNewA).build();
        List<ItemUpdate> scheduled = schedule(updateA);
        ItemUpdate newUpdate = new ItemUpdateBuilder(newIdA).build();
        assertEquals(Arrays.asList(newUpdate, updateA), scheduled);
    }
    
    @Test
    public void testNewItemMentioned() throws ImpossibleSchedulingException {
        ItemUpdate updateA = new ItemUpdateBuilder(existingIdA).addStatement(sAtoNewA).build();
        ItemUpdate newUpdate = new ItemUpdateBuilder(newIdA).addStatement(sNewAtoB).build();
        List<ItemUpdate> scheduled = schedule(updateA, newUpdate);
        assertEquals(Arrays.asList(newUpdate, updateA), scheduled);
    }
    
    @Test
    public void testMerge() throws ImpossibleSchedulingException {
        ItemUpdate update1 = new ItemUpdateBuilder(existingIdA)
                .addStatement(sAtoB)
                .build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"))
                .addStatement(sAtoB)
                .build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singletonList(merged), schedule(update1, update2));
    }
    
    @Test
    public void testMergeNew() throws ImpossibleSchedulingException {
        ItemUpdate update1 = new ItemUpdateBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"))
                .addStatement(sNewAtoB)
                .build();
        ItemUpdate update2 = new ItemUpdateBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("hello", "fr"))
                .build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singletonList(merged), schedule(update1, update2));
    }
}
