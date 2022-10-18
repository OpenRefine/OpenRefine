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

package org.openrefine.wikibase.updates;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.google.refine.util.TestUtils;

public class ItemEditTest {

    private ItemIdValue existingSubject = Datamodel.makeWikidataItemIdValue("Q34");
    private ItemIdValue otherExistingSubject = Datamodel.makeWikidataItemIdValue("Q56");
    private ItemIdValue newSubject = TestingData.makeNewItemIdValue(1234L, "new item");

    private PropertyIdValue pid1 = Datamodel.makeWikidataPropertyIdValue("P348");
    private PropertyIdValue pid2 = Datamodel.makeWikidataPropertyIdValue("P52");
    private Claim claim1 = Datamodel.makeClaim(existingSubject, Datamodel.makeNoValueSnak(pid1),
            Collections.emptyList());
    private Claim claim1WithOtherSubject = Datamodel.makeClaim(otherExistingSubject, Datamodel.makeNoValueSnak(pid1),
            Collections.emptyList());
    private Claim claim2 = Datamodel.makeClaim(existingSubject, Datamodel.makeValueSnak(pid2, newSubject),
            Collections.emptyList());
    private Claim claim2WithOtherSubject = Datamodel.makeClaim(otherExistingSubject, Datamodel.makeValueSnak(pid2, newSubject),
            Collections.emptyList());
    private Statement statement1 = Datamodel.makeStatement(claim1, Collections.emptyList(), StatementRank.NORMAL, "");
    private Statement statement1WithOtherSubject = Datamodel.makeStatement(claim1WithOtherSubject, Collections.emptyList(),
            StatementRank.NORMAL, "");
    private Statement statement2 = Datamodel.makeStatement(claim2, Collections.emptyList(), StatementRank.NORMAL, "");
    private Statement statement2WithOtherSubject = Datamodel.makeStatement(claim2WithOtherSubject, Collections.emptyList(),
            StatementRank.NORMAL, "");
    private StatementMerger strategy = new PropertyOnlyStatementMerger();
    private StatementEdit statementUpdate1 = new StatementEdit(statement1, strategy, StatementEditingMode.ADD_OR_MERGE);
    private StatementEdit statementUpdate2 = new StatementEdit(statement2, strategy, StatementEditingMode.DELETE);
    private MonolingualTextValue label = Datamodel.makeMonolingualTextValue("this is a label", "en");

    private Set<StatementGroupEdit> statementGroups;

    public ItemEditTest() {
        statementGroups = new HashSet<>();
        statementGroups.add(new StatementGroupEdit(Collections.singletonList(statementUpdate1)));
        statementGroups.add(new StatementGroupEdit(Collections.singletonList(statementUpdate2)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateWithoutSubject() {
        new ItemEditBuilder(null);
    }

    @Test
    public void testIsNull() {
        ItemEdit update = new ItemEditBuilder(existingSubject).build();
        assertTrue(update.isNull());
        ItemEdit update2 = new ItemEditBuilder(newSubject).build();
        assertFalse(update2.isNull());
    }

    @Test
    public void testIsEmpty() {
        ItemEdit update = new ItemEditBuilder(existingSubject).build();
        assertTrue(update.isEmpty());
        ItemEdit update2 = new ItemEditBuilder(newSubject).build();
        assertTrue(update2.isEmpty());
    }

    @Test
    public void testIsNew() {
        ItemEdit newUpdate = new ItemEditBuilder(newSubject).build();
        assertTrue(newUpdate.isNew());
        ItemEdit update = new ItemEditBuilder(existingSubject).build();
        assertFalse(update.isNew());
    }

    @Test
    public void testAddStatements() {
        ItemEdit update = new ItemEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        assertFalse(update.isNull());
        assertEquals(Arrays.asList(statementUpdate1, statementUpdate2), update.getStatementEdits());
        assertEquals(statementGroups, update.getStatementGroupEdits().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testSerializeStatements() throws IOException {
        ItemEdit update = new ItemEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        TestUtils.isSerializedTo(update, TestingData.jsonFromFile("updates/entity_update.json"));
    }

    @Test
    public void testMerge() {
        ItemEdit updateA = new ItemEditBuilder(existingSubject).addStatement(statementUpdate1).build();
        ItemEdit updateB = new ItemEditBuilder(existingSubject).addStatement(statementUpdate2).build();
        assertNotEquals(updateA, updateB);
        ItemEdit merged = updateA.merge(updateB);
        assertEquals(statementGroups, merged.getStatementGroupEdits().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testNormalizeTerms() {
        MonolingualTextValue aliasEn = Datamodel.makeMonolingualTextValue("alias", "en");
        MonolingualTextValue aliasFr = Datamodel.makeMonolingualTextValue("coucou", "fr");
        ItemEdit updateA = new ItemEditBuilder(newSubject).addLabel(label, true).addAlias(aliasEn)
                .addAlias(aliasFr)
                .build();
        assertFalse(updateA.isNull());
        ItemDocument normalized = updateA.toNewEntity();
        ItemDocument expectedDocument = ItemDocumentBuilder.forItemId(newSubject).withLabel(label)
                .withAlias(aliasEn)
                .withLabel(aliasFr).build();
        assertEquals(expectedDocument, normalized);
    }

    @Test
    public void testMergeLabels() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemEdit edit1 = new ItemEditBuilder(existingSubject).addLabel(label1, true).build();
        ItemEdit edit2 = new ItemEditBuilder(existingSubject).addLabel(label2, true).build();
        ItemEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label2), merged.getLabels());
    }

    @Test
    public void testMergeLabelsIfNew() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemEdit edit1 = new ItemEditBuilder(existingSubject).addLabel(label1, false).build();
        ItemEdit edit2 = new ItemEditBuilder(existingSubject).addLabel(label2, false).build();
        ItemEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label1), merged.getLabelsIfNew());
        assertEquals(Collections.emptySet(), merged.getLabels());
    }

    @Test
    public void testMergeLabelsIfNewOverriding() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemEdit edit1 = new ItemEditBuilder(existingSubject).addLabel(label1, true).build();
        ItemEdit edit2 = new ItemEditBuilder(existingSubject).addLabel(label2, false).build();
        ItemEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label1), merged.getLabels());
        assertEquals(Collections.emptySet(), merged.getLabelsIfNew());
    }

    @Test
    public void testMergeLabelsIfNewOverriding2() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        ItemEdit edit1 = new ItemEditBuilder(existingSubject).addLabel(label1, false).build();
        ItemEdit edit2 = new ItemEditBuilder(existingSubject).addLabel(label2, true).build();
        ItemEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(label2), merged.getLabels());
        assertEquals(Collections.emptySet(), merged.getLabelsIfNew());
    }

    @Test
    public void testMergeDescriptionsIfNew() {
        MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        ItemEdit edit1 = new ItemEditBuilder(existingSubject).addDescription(description1, false)
                .build();
        ItemEdit edit2 = new ItemEditBuilder(existingSubject).addDescription(description2, false)
                .build();
        ItemEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(description1), merged.getDescriptionsIfNew());
        assertEquals(Collections.emptySet(), merged.getDescriptions());
        assertFalse(merged.isEmpty());
    }

    @Test
    public void testMergeDescriptionsIfNewOverriding() {
        MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        ItemEdit edit1 = new ItemEditBuilder(existingSubject).addDescription(description1, true)
                .build();
        ItemEdit edit2 = new ItemEditBuilder(existingSubject).addDescription(description2, false)
                .build();
        ItemEdit merged = edit1.merge(edit2);
        assertEquals(Collections.singleton(description1), merged.getDescriptions());
        assertEquals(Collections.emptySet(), merged.getDescriptionsIfNew());
    }

    @Test
    public void testMergeDescriptionsIfNewOverriding2() {
        MonolingualTextValue description1 = Datamodel.makeMonolingualTextValue("first description", "en");
        MonolingualTextValue description2 = Datamodel.makeMonolingualTextValue("second description", "en");
        ItemEdit update1 = new ItemEditBuilder(existingSubject).addDescription(description1, false)
                .build();
        ItemEdit update2 = new ItemEditBuilder(existingSubject).addDescription(description2, true)
                .build();
        ItemEdit merged = update1.merge(update2);
        assertEquals(Collections.singleton(description2), merged.getDescriptions());
        assertEquals(Collections.emptySet(), merged.getDescriptionsIfNew());
    }

    @Test
    public void testConstructOverridingLabels() {
        MonolingualTextValue label1 = Datamodel.makeMonolingualTextValue("first label", "en");
        MonolingualTextValue label2 = Datamodel.makeMonolingualTextValue("second label", "en");
        LabeledStatementEntityEdit update = new ItemEditBuilder(existingSubject)
                .addLabel(label1, false)
                .addLabel(label2, true)
                .build();
        assertEquals(Collections.singleton(label2), update.getLabels());
        assertEquals(Collections.emptySet(), update.getLabelsIfNew());
    }

    @Test
    public void testToEntityUpdate() {
        TermedStatementEntityEdit edit = new ItemEditBuilder(existingSubject)
                .addAlias(Datamodel.makeMonolingualTextValue("alias", "en"))
                .addStatement(statementUpdate1)
                .build();
        ItemDocument itemDocument = ItemDocumentBuilder.forItemId(otherExistingSubject)
                .withStatement(statement2WithOtherSubject)
                .withLabel(Datamodel.makeMonolingualTextValue("label", "en"))
                .build();

        ItemUpdate update = (ItemUpdate) edit.toEntityUpdate(itemDocument);
        assertEquals(update.getEntityId(), otherExistingSubject); // note the difference between the subjects:
        // the subject from the document is fresher, in case of redirects.
        assertEquals(update.getAliases(), Collections.singletonMap("en",
                Datamodel.makeAliasUpdate(Collections.singletonList(Datamodel.makeMonolingualTextValue("alias", "en")),
                        Collections.emptyList())));
        assertTrue(update.getDescriptions().isEmpty());
        assertTrue(update.getLabels().isEmpty());
        assertEquals(update.getStatements(),
                Datamodel.makeStatementUpdate(Collections.singletonList(statement1WithOtherSubject), Collections.emptyList(),
                        Collections.emptySet()));
    }

    @Test
    public void testToNewEntity() {
        TermedStatementEntityEdit edit = new ItemEditBuilder(newSubject)
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
