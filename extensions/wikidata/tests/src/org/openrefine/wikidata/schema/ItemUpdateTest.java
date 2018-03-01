package org.openrefine.wikidata.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public class ItemUpdateTest {
    
    private ItemIdValue existingSubject = Datamodel.makeWikidataItemIdValue("Q34");
    private ItemIdValue newSubject = TestingDataGenerator.makeNewItemIdValue(1234L, "new item");
    private ItemIdValue sameNewSubject = TestingDataGenerator.makeNewItemIdValue(1234L, "other new item");
    private ItemIdValue matchedSubject = TestingDataGenerator.makeMatchedItemIdValue("Q78", "well known item");
    private ItemUpdate update = new ItemUpdate(existingSubject);
    
    private PropertyIdValue pid1 = Datamodel.makeWikidataPropertyIdValue("P348");
    private PropertyIdValue pid2 = Datamodel.makeWikidataPropertyIdValue("P52");
    private Claim claim1 = Datamodel.makeClaim(existingSubject,
            Datamodel.makeNoValueSnak(pid1), Collections.emptyList());
    private Claim claim2 = Datamodel.makeClaim(existingSubject,
            Datamodel.makeValueSnak(pid2, newSubject), Collections.emptyList());
    private Statement statement1 = Datamodel.makeStatement(claim1,
            Collections.emptyList(), StatementRank.NORMAL, "");
    private Statement statement2 = Datamodel.makeStatement(claim2,
            Collections.emptyList(), StatementRank.NORMAL, "");
    private MonolingualTextValue label = Datamodel.makeMonolingualTextValue("this is a label", "en");
    
    private Set<StatementGroup> statementGroups;
    
    public ItemUpdateTest() {
        statementGroups = new HashSet<>();
        statementGroups.add(Datamodel.makeStatementGroup(Collections.singletonList(statement1)));
        statementGroups.add(Datamodel.makeStatementGroup(Collections.singletonList(statement2)));
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreateWithoutSubject() {
        new ItemUpdate(null);
    }
    
    @Test
    public void testIsNull() {
        assertTrue(update.isNull());
    }
    
    @Test
    public void testIsNew() {
        ItemUpdate newUpdate = new ItemUpdate(newSubject);
        assertTrue(newUpdate.isNew());
        assertFalse(update.isNew());
    }
    
    @Test
    public void testAddStatements() {
        ItemUpdate update = new ItemUpdate(existingSubject);
        update.addStatement(statement1);
        update.addStatement(statement2);
        assertEquals(Arrays.asList(statement1, statement2).stream().collect(Collectors.toSet()), 
                update.getAddedStatements());
        assertEquals(statementGroups, update.getAddedStatementGroups().stream().collect(Collectors.toSet()));
    }
    
    @Test
    public void testDeleteStatements() {
        ItemUpdate update = new ItemUpdate(existingSubject);
        update.deleteStatement(statement1);
        update.deleteStatement(statement2);
        assertEquals(Arrays.asList(statement1, statement2).stream().collect(Collectors.toSet()),
                update.getDeletedStatements());
    }
    
    @Test
    public void testMerge() {
        ItemUpdate updateA = new ItemUpdate(existingSubject);
        updateA.addStatement(statement1);
        ItemUpdate updateB = new ItemUpdate(existingSubject);
        updateB.addStatement(statement2);
        assertNotEquals(updateA, updateB);
        updateA.merge(updateB);
        assertEquals(statementGroups,
                updateA.getAddedStatementGroups().stream().collect(Collectors.toSet()));
    }
    
    @Test
    public void testGroupBySubject() {
        ItemUpdate updateA = new ItemUpdate(newSubject);
        updateA.addStatement(statement1);
        ItemUpdate updateB = new ItemUpdate(sameNewSubject);
        updateB.addStatement(statement2);
        ItemUpdate updateC = new ItemUpdate(existingSubject);
        updateC.addLabel(label);
        ItemUpdate updateD = new ItemUpdate(matchedSubject);
        Map<EntityIdValue, ItemUpdate> grouped = ItemUpdate.groupBySubject(
                Arrays.asList(updateA, updateB, updateC, updateD));
        ItemUpdate mergedUpdate = new ItemUpdate(newSubject);
        mergedUpdate.addStatement(statement1);
        mergedUpdate.addStatement(statement2);
        Map<EntityIdValue, ItemUpdate> expected = new HashMap<>();
        expected.put(newSubject, mergedUpdate);
        expected.put(existingSubject, updateC);
        assertEquals(expected, grouped);
    }
    
    @Test
    public void testNormalizeTerms() {
        MonolingualTextValue aliasEn = Datamodel.makeMonolingualTextValue("alias", "en");
        MonolingualTextValue aliasFr = Datamodel.makeMonolingualTextValue("coucou", "fr");
        ItemUpdate updateA = new ItemUpdate(newSubject);
        updateA.addLabel(label);
        updateA.addAlias(aliasEn);
        updateA.addAlias(aliasFr);
        updateA.normalizeLabelsAndAliases();
        ItemUpdate expectedUpdate = new ItemUpdate(newSubject);
        expectedUpdate.addLabel(label);
        expectedUpdate.addAlias(aliasEn);
        expectedUpdate.addLabel(aliasFr);
        assertEquals(expectedUpdate, updateA);
    }
}
