
package org.openrefine.wikibase.updates;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.testing.TestingData;

public class EntityEditTest {

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

    @Test
    public void testGroupBySubject() {
        TermedStatementEntityEdit updateA = new ItemEditBuilder(newSubject).addStatement(statementUpdate1).addContributingRowId(123)
                .build();
        TermedStatementEntityEdit updateB = new ItemEditBuilder(sameNewSubject).addStatement(statementUpdate2).addContributingRowId(123)
                .build();
        TermedStatementEntityEdit updateC = new ItemEditBuilder(existingSubject).addLabel(label, true).addContributingRowId(123).build();
        TermedStatementEntityEdit updateD = new ItemEditBuilder(matchedSubject).addContributingRowId(123).build();
        Map<EntityIdValue, EntityEdit> grouped = EntityEdit
                .groupBySubject(Arrays.asList(updateA, updateB, updateC, updateD));
        TermedStatementEntityEdit mergedUpdate = new ItemEditBuilder(newSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .addContributingRowId(123)
                .build();
        Map<EntityIdValue, TermedStatementEntityEdit> expected = new HashMap<>();
        expected.put(newSubject, mergedUpdate);
        expected.put(existingSubject, updateC);
        assertEquals(expected, grouped);
    }
}
