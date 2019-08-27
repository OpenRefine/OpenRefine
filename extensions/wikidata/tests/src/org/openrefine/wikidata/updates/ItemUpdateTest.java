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
package org.openrefine.wikidata.updates;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.testing.TestingData;
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

import com.google.refine.util.TestUtils;

public class ItemUpdateTest {

    private ItemIdValue existingSubject = Datamodel.makeWikidataItemIdValue("Q34");
    private ItemIdValue newSubject = TestingData.makeNewItemIdValue(1234L, "new item");
    private ItemIdValue sameNewSubject = TestingData.makeNewItemIdValue(1234L, "other new item");
    private ItemIdValue matchedSubject = TestingData.makeMatchedItemIdValue("Q78", "well known item");

    private PropertyIdValue pid1 = Datamodel.makeWikidataPropertyIdValue("P348");
    private PropertyIdValue pid2 = Datamodel.makeWikidataPropertyIdValue("P52");
    private Claim claim1 = Datamodel.makeClaim(existingSubject, Datamodel.makeNoValueSnak(pid1),
            Collections.emptyList());
    private Claim claim2 = Datamodel.makeClaim(existingSubject, Datamodel.makeValueSnak(pid2, newSubject),
            Collections.emptyList());
    private Statement statement1 = Datamodel.makeStatement(claim1, Collections.emptyList(), StatementRank.NORMAL, "");
    private Statement statement2 = Datamodel.makeStatement(claim2, Collections.emptyList(), StatementRank.NORMAL, "");
    private MonolingualTextValue label = Datamodel.makeMonolingualTextValue("this is a label", "en");

    private Set<StatementGroup> statementGroups;

    public ItemUpdateTest() {
        statementGroups = new HashSet<>();
        statementGroups.add(Datamodel.makeStatementGroup(Collections.singletonList(statement1)));
        statementGroups.add(Datamodel.makeStatementGroup(Collections.singletonList(statement2)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateWithoutSubject() {
        new ItemUpdateBuilder(null);
    }

    @Test
    public void testIsNull() {
        ItemUpdate update = new ItemUpdateBuilder(existingSubject).build();
        assertTrue(update.isNull());
        ItemUpdate update2 = new ItemUpdateBuilder(newSubject).build();
        assertFalse(update2.isNull());
    }

    @Test
    public void testIsEmpty() {
        ItemUpdate update = new ItemUpdateBuilder(existingSubject).build();
        assertTrue(update.isEmpty());
        ItemUpdate update2 = new ItemUpdateBuilder(newSubject).build();
        assertTrue(update2.isEmpty());
    }

    @Test
    public void testIsNew() {
        ItemUpdate newUpdate = new ItemUpdateBuilder(newSubject).build();
        assertTrue(newUpdate.isNew());
        ItemUpdate update = new ItemUpdateBuilder(existingSubject).build();
        assertFalse(update.isNew());
    }

    @Test
    public void testAddStatements() {
        ItemUpdate update = new ItemUpdateBuilder(existingSubject).addStatement(statement1).addStatement(statement2)
                .build();
        assertFalse(update.isNull());
        assertEquals(Arrays.asList(statement1, statement2), update.getAddedStatements());
        assertEquals(statementGroups, update.getAddedStatementGroups().stream().collect(Collectors.toSet()));
    }
    
    /**
     * Test disabled because it fails due to
     * https://github.com/Wikidata/Wikidata-Toolkit/issues/417
     * (not fixed as of WDTK 0.10.0).
     * 
     * This bug is not critical as the extraneous serialized data
     * is ignored by Wikibase.
     * 
     * @todo reenable once a later version is released
     */
    @Test(enabled=false)
    public void testSerializeStatements() throws IOException {
    	ItemUpdate update = new ItemUpdateBuilder(existingSubject).addStatement(statement1).addStatement(statement2)
                .build();
    	TestUtils.isSerializedTo(update, TestingData.jsonFromFile("updates/statement_groups.json"));
    }

    @Test
    public void testDeleteStatements() {
        ItemUpdate update = new ItemUpdateBuilder(existingSubject).deleteStatement(statement1)
                .deleteStatement(statement2).build();
        assertEquals(Arrays.asList(statement1, statement2).stream().collect(Collectors.toSet()),
                update.getDeletedStatements());
    }

    @Test
    public void testMerge() {
        ItemUpdate updateA = new ItemUpdateBuilder(existingSubject).addStatement(statement1).build();
        ItemUpdate updateB = new ItemUpdateBuilder(existingSubject).addStatement(statement2).build();
        assertNotEquals(updateA, updateB);
        ItemUpdate merged = updateA.merge(updateB);
        assertEquals(statementGroups, merged.getAddedStatementGroups().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testGroupBySubject() {
        ItemUpdate updateA = new ItemUpdateBuilder(newSubject).addStatement(statement1).build();
        ItemUpdate updateB = new ItemUpdateBuilder(sameNewSubject).addStatement(statement2).build();
        ItemUpdate updateC = new ItemUpdateBuilder(existingSubject).addLabel(label, true).build();
        ItemUpdate updateD = new ItemUpdateBuilder(matchedSubject).build();
        Map<EntityIdValue, ItemUpdate> grouped = ItemUpdate
                .groupBySubject(Arrays.asList(updateA, updateB, updateC, updateD));
        ItemUpdate mergedUpdate = new ItemUpdateBuilder(newSubject).addStatement(statement1).addStatement(statement2)
                .build();
        Map<EntityIdValue, ItemUpdate> expected = new HashMap<>();
        expected.put(newSubject, mergedUpdate);
        expected.put(existingSubject, updateC);
        assertEquals(expected, grouped);
    }

    @Test
    public void testNormalizeTerms() {
        MonolingualTextValue aliasEn = Datamodel.makeMonolingualTextValue("alias", "en");
        MonolingualTextValue aliasFr = Datamodel.makeMonolingualTextValue("coucou", "fr");
        ItemUpdate updateA = new ItemUpdateBuilder(newSubject).addLabel(label, true).addAlias(aliasEn).addAlias(aliasFr)
                .build();
        assertFalse(updateA.isNull());
        ItemUpdate normalized = updateA.normalizeLabelsAndAliases();
        ItemUpdate expectedUpdate = new ItemUpdateBuilder(newSubject).addLabel(label, true).addAlias(aliasEn)
                .addLabel(aliasFr, true).build();
        assertEquals(expectedUpdate, normalized);
    }
    
    @Test
    public void testMergeLabels() {
    	MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addLabel(label1, true).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addLabel(label2, true).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(label2), merged.getLabels());
    }
    
    @Test
    public void testMergeLabelsIfNew() {
    	MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addLabel(label1, false).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addLabel(label2, false).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(label1), merged.getLabelsIfNew());
        assertEquals(Collections.emptySet(), merged.getLabels());
    }
    
    @Test
    public void testMergeLabelsIfNewOverriding() {
    	MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addLabel(label1, true).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addLabel(label2, false).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(label1), merged.getLabels());
        assertEquals(Collections.emptySet(), merged.getLabelsIfNew());
    }
    
    @Test
    public void testMergeLabelsIfNewOverriding2() {
    	MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addLabel(label1, false).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addLabel(label2, true).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(label2), merged.getLabels());
        assertEquals(Collections.emptySet(), merged.getLabelsIfNew());
    }
    
    @Test
    public void testMergeDescriptionsIfNew() {
    	MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addDescription(description1, false).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addDescription(description2, false).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(description1), merged.getDescriptionsIfNew());
        assertEquals(Collections.emptySet(), merged.getDescriptions());
        assertFalse(merged.isEmpty());
    }
    
    @Test
    public void testMergeDescriptionsIfNewOverriding() {
    	MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addDescription(description1, true).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addDescription(description2, false).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(description1), merged.getDescriptions());
        assertEquals(Collections.emptySet(), merged.getDescriptionsIfNew());
    }
    
    @Test
    public void testMergeDescriptionsIfNewOverriding2() {
    	MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        ItemUpdate update1 = new ItemUpdateBuilder(existingSubject).addDescription(description1, false).build();
        ItemUpdate update2 = new ItemUpdateBuilder(existingSubject).addDescription(description2, true).build();
        ItemUpdate merged = update1.merge(update2);
        assertEquals(Collections.singleton(description2), merged.getDescriptions());
        assertEquals(Collections.emptySet(), merged.getDescriptionsIfNew());
    }
    
    @Test
    public void testConstructOverridingLabels() {
    	MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemUpdate update = new ItemUpdateBuilder(existingSubject)
        		.addLabel(label1, false)
        		.addLabel(label2, true)
        		.build();
        assertEquals(Collections.singleton(label2), update.getLabels());
        assertEquals(Collections.emptySet(), update.getLabelsIfNew());
    }
}
