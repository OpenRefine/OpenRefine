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

import org.openrefine.wikidata.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikidata.schema.strategies.StatementEditingMode;
import org.openrefine.wikidata.schema.strategies.StatementMerger;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.google.refine.util.TestUtils;

public class TermedStatementEntityEditTest {

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
    private StatementMerger strategy = new PropertyOnlyStatementMerger();
    private StatementEdit statementUpdate1 = new StatementEdit(statement1, strategy, StatementEditingMode.ADD_OR_MERGE);
    private StatementEdit statementUpdate2 = new StatementEdit(statement2, strategy, StatementEditingMode.DELETE);
    private MonolingualTextValue label = Datamodel.makeMonolingualTextValue("this is a label", "en");

    private Set<StatementGroupEdit> statementGroups;

    public TermedStatementEntityEditTest() {
        statementGroups = new HashSet<>();
        statementGroups.add(new StatementGroupEdit(Collections.singletonList(statementUpdate1)));
        statementGroups.add(new StatementGroupEdit(Collections.singletonList(statementUpdate2)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateWithoutSubject() {
        new TermedStatementEntityEditBuilder(null);
    }

    @Test
    public void testIsNull() {
        TermedStatementEntityEdit update = new TermedStatementEntityEditBuilder(existingSubject).build();
        assertTrue(update.isNull());
        TermedStatementEntityEdit update2 = new TermedStatementEntityEditBuilder(newSubject).build();
        assertFalse(update2.isNull());
    }

    @Test
    public void testIsEmpty() {
        TermedStatementEntityEdit update = new TermedStatementEntityEditBuilder(existingSubject).build();
        assertTrue(update.isEmpty());
        TermedStatementEntityEdit update2 = new TermedStatementEntityEditBuilder(newSubject).build();
        assertTrue(update2.isEmpty());
    }

    @Test
    public void testIsNew() {
        TermedStatementEntityEdit newUpdate = new TermedStatementEntityEditBuilder(newSubject).build();
        assertTrue(newUpdate.isNew());
        TermedStatementEntityEdit update = new TermedStatementEntityEditBuilder(existingSubject).build();
        assertFalse(update.isNew());
    }

    @Test
    public void testAddStatements() {
        TermedStatementEntityEdit update = new TermedStatementEntityEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        assertFalse(update.isNull());
        assertEquals(Arrays.asList(statementUpdate1, statementUpdate2), update.getStatementEdits());
        assertEquals(statementGroups, update.getStatementGroupEdits().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testSerializeStatements() throws IOException {
        TermedStatementEntityEdit update = new TermedStatementEntityEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        TestUtils.isSerializedTo(update, TestingData.jsonFromFile("updates/entity_update.json"));
    }

    @Test
    public void testMerge() {
        TermedStatementEntityEdit updateA = new TermedStatementEntityEditBuilder(existingSubject).addStatement(statementUpdate1).build();
        TermedStatementEntityEdit updateB = new TermedStatementEntityEditBuilder(existingSubject).addStatement(statementUpdate2).build();
        assertNotEquals(updateA, updateB);
        TermedStatementEntityEdit merged = updateA.merge(updateB);
        assertEquals(statementGroups, merged.getStatementGroupEdits().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testGroupBySubject() {
        TermedStatementEntityEdit updateA = new TermedStatementEntityEditBuilder(newSubject).addStatement(statementUpdate1).build();
        TermedStatementEntityEdit updateB = new TermedStatementEntityEditBuilder(sameNewSubject).addStatement(statementUpdate2).build();
        TermedStatementEntityEdit updateC = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label, true).build();
        TermedStatementEntityEdit updateD = new TermedStatementEntityEditBuilder(matchedSubject).build();
        Map<EntityIdValue, TermedStatementEntityEdit> grouped = TermedStatementEntityEdit
                .groupBySubject(Arrays.asList(updateA, updateB, updateC, updateD));
        TermedStatementEntityEdit mergedUpdate = new TermedStatementEntityEditBuilder(newSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        Map<EntityIdValue, TermedStatementEntityEdit> expected = new HashMap<>();
        expected.put(newSubject, mergedUpdate);
        expected.put(existingSubject, updateC);
        assertEquals(expected, grouped);
    }

    @Test
    public void testNormalizeTerms() {
        MonolingualTextValue aliasEn = Datamodel.makeMonolingualTextValue("alias", "en");
        MonolingualTextValue aliasFr = Datamodel.makeMonolingualTextValue("coucou", "fr");
        TermedStatementEntityEdit updateA = new TermedStatementEntityEditBuilder(newSubject).addLabel(label, true).addAlias(aliasEn)
                .addAlias(aliasFr)
                .build();
        assertFalse(updateA.isNull());
        TermedStatementEntityEdit normalized = updateA.normalizeLabelsAndAliases();
        TermedStatementEntityEdit expectedUpdate = new TermedStatementEntityEditBuilder(newSubject).addLabel(label, true)
                .addAlias(aliasEn)
                .addLabel(aliasFr, true).build();
        assertEquals(expectedUpdate, normalized);
    }

    @Test
    public void testMergeLabels() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        TermedStatementEntityEdit edit1 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label1, true).build();
        TermedStatementEntityEdit edit2 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label2, true).build();
        TermedStatementEntityEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label2), merged.getLabels());
    }

    @Test
    public void testMergeLabelsIfNew() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        TermedStatementEntityEdit edit1 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label1, false).build();
        TermedStatementEntityEdit edit2 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label2, false).build();
        TermedStatementEntityEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label1), merged.getLabelsIfNew());
        assertEquals(Collections.emptySet(), merged.getLabels());
    }

    @Test
    public void testMergeLabelsIfNewOverriding() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        TermedStatementEntityEdit edit1 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label1, true).build();
        TermedStatementEntityEdit edit2 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label2, false).build();
        TermedStatementEntityEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label1), merged.getLabels());
        assertEquals(Collections.emptySet(), merged.getLabelsIfNew());
    }

    @Test
    public void testMergeLabelsIfNewOverriding2() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        TermedStatementEntityEdit edit1 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label1, false).build();
        TermedStatementEntityEdit edit2 = new TermedStatementEntityEditBuilder(existingSubject).addLabel(label2, true).build();
        TermedStatementEntityEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label2), merged.getLabels());
        assertEquals(Collections.emptySet(), merged.getLabelsIfNew());
    }

    @Test
    public void testMergeDescriptionsIfNew() {
        MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        TermedStatementEntityEdit edit1 = new TermedStatementEntityEditBuilder(existingSubject).addDescription(description1, false)
                .build();
        TermedStatementEntityEdit edit2 = new TermedStatementEntityEditBuilder(existingSubject).addDescription(description2, false)
                .build();
        TermedStatementEntityEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(description1), merged.getDescriptionsIfNew());
        assertEquals(Collections.emptySet(), merged.getDescriptions());
        assertFalse(merged.isEmpty());
    }

    @Test
    public void testMergeDescriptionsIfNewOverriding() {
        MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        TermedStatementEntityEdit edit1 = new TermedStatementEntityEditBuilder(existingSubject).addDescription(description1, true)
                .build();
        TermedStatementEntityEdit edit2 = new TermedStatementEntityEditBuilder(existingSubject).addDescription(description2, false)
                .build();
        TermedStatementEntityEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(description1), merged.getDescriptions());
        assertEquals(Collections.emptySet(), merged.getDescriptionsIfNew());
    }

    @Test
    public void testMergeDescriptionsIfNewOverriding2() {
        MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        TermedStatementEntityEdit update1 = new TermedStatementEntityEditBuilder(existingSubject).addDescription(description1, false)
                .build();
        TermedStatementEntityEdit update2 = new TermedStatementEntityEditBuilder(existingSubject).addDescription(description2, true)
                .build();
        TermedStatementEntityEdit merged = update1.merge(update2);
        assertEquals(Collections.singleton(description2), merged.getDescriptions());
        assertEquals(Collections.emptySet(), merged.getDescriptionsIfNew());
    }

    @Test
    public void testConstructOverridingLabels() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        TermedStatementEntityEdit update = new TermedStatementEntityEditBuilder(existingSubject)
                .addLabel(label1, false)
                .addLabel(label2, true)
                .build();
        assertEquals(Collections.singleton(label2), update.getLabels());
        assertEquals(Collections.emptySet(), update.getLabelsIfNew());
    }

    @Test
    public void testToEntityUpdate() {
        TermedStatementEntityEdit edit = new TermedStatementEntityEditBuilder(existingSubject)
                .addAlias(Datamodel.makeMonolingualTextValue("alias", "en"))
                .addStatement(statementUpdate1)
                .build();
        ItemDocument itemDocument = ItemDocumentBuilder.forItemId(existingSubject)
                .withStatement(statement2)
                .withLabel(Datamodel.makeMonolingualTextValue("label", "en"))
                .build();

        ItemUpdate update = (ItemUpdate) edit.toEntityUpdate(itemDocument);
        assertEquals(update.getEntityId(), existingSubject);
        assertEquals(update.getAliases(), Collections.singletonMap("en",
                Datamodel.makeAliasUpdate(Collections.singletonList(Datamodel.makeMonolingualTextValue("alias", "en")),
                        Collections.emptyList())));
        assertTrue(update.getDescriptions().isEmpty());
        assertTrue(update.getLabels().isEmpty());
        assertEquals(update.getStatements(),
                Datamodel.makeStatementUpdate(Collections.singletonList(statement1), Collections.emptyList(), Collections.emptySet()));
    }

    @Test
    public void testToNewEntity() {
        TermedStatementEntityEdit edit = new TermedStatementEntityEditBuilder(newSubject)
                .addLabel(Datamodel.makeMonolingualTextValue("fr", "bonjour"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("de", "Redewendung"), true)
                .build();

        ItemDocument itemDocument = (ItemDocument) edit.toNewEntity();

        ItemDocument expected = ItemDocumentBuilder.forItemId(newSubject)
                .withLabel(Datamodel.makeMonolingualTextValue("fr", "bonjour"))
                .withDescription(Datamodel.makeMonolingualTextValue("de", "Redewendung"))
                .build();
        assertEquals(itemDocument, expected);
    }
}
