
package org.openrefine.wikibase.updates;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.StatementUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;

import com.google.refine.util.TestUtils;

public class StatementGroupEditTest {

    Statement statement = TestingData.generateStatement(TestingData.existingId, TestingData.newIdA);
    StatementMerger strategy = new PropertyOnlyStatementMerger();
    StatementEditingMode mode = StatementEditingMode.ADD_OR_MERGE;
    StatementEdit statementEdit = new StatementEdit(statement, strategy, mode);
    StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

    String statementId1 = "statementId1";
    Statement statement1;
    String statementId2 = "statementId2";
    Statement statement2;
    StatementMerger merger;
    StatementGroup statementGroup;
    StatementUpdateBuilder builder;

    @BeforeMethod
    public void setUpMocks() {
        statement1 = mock(Statement.class);
        when(statement1.getStatementId()).thenReturn(statementId1);
        when(statement1.getSubject()).thenReturn(TestingData.existingId);
        statement2 = mock(Statement.class);
        when(statement2.getStatementId()).thenReturn(statementId2);
        when(statement2.getSubject()).thenReturn(TestingData.existingId);
        merger = mock(StatementMerger.class);
        statementGroup = mock(StatementGroup.class);
        when(statementGroup.getStatements()).thenReturn(Arrays.asList(statement1, statement2));

        builder = StatementUpdateBuilder.create(TestingData.existingId);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructor() {
        new StatementGroupEdit(Collections.emptyList());
    }

    @Test
    public void testDeleteStatements() {
        when(merger.match(statement1, statement)).thenReturn(false);
        when(merger.match(statement2, statement)).thenReturn(true);
        StatementEdit statementEdit = new StatementEdit(statement, merger, StatementEditingMode.DELETE);
        StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

        SUT.contributeToStatementUpdate(builder, statementGroup);

        StatementUpdate statementUpdate = builder.build();
        assertEquals(statementUpdate.getAdded(), Collections.emptyList());
        assertEquals(statementUpdate.getReplaced(), Collections.emptyMap());
        assertEquals(statementUpdate.getRemoved(), Collections.singleton(statementId2));
    }

    @Test
    public void testAddStatementsNoMatching() {
        when(merger.match(statement1, statement)).thenReturn(false);
        when(merger.match(statement2, statement)).thenReturn(false);
        StatementEdit statementEdit = new StatementEdit(statement, merger, StatementEditingMode.ADD);
        StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

        SUT.contributeToStatementUpdate(builder, statementGroup);

        StatementUpdate statementUpdate = builder.build();
        assertEquals(statementUpdate.getAdded(), Collections.singletonList(statement));
        assertEquals(statementUpdate.getReplaced(), Collections.emptyMap());
        assertEquals(statementUpdate.getRemoved(), Collections.emptySet());
    }

    @Test
    public void testAddStatementsMatching() {
        when(merger.match(statement1, statement)).thenReturn(true);
        when(merger.match(statement2, statement)).thenReturn(false);
        StatementEdit statementEdit = new StatementEdit(statement, merger, StatementEditingMode.ADD);
        StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

        SUT.contributeToStatementUpdate(builder, statementGroup);

        StatementUpdate statementUpdate = builder.build();
        assertEquals(statementUpdate.getAdded(), Collections.emptyList());
        assertEquals(statementUpdate.getReplaced(), Collections.emptyMap());
        assertEquals(statementUpdate.getRemoved(), Collections.emptySet());
    }

    @Test
    public void testAddOrMergeStatementsNoMatching() {
        when(merger.match(statement1, statement)).thenReturn(false);
        when(merger.match(statement2, statement)).thenReturn(false);
        StatementEdit statementEdit = new StatementEdit(statement, merger, StatementEditingMode.ADD_OR_MERGE);
        StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

        SUT.contributeToStatementUpdate(builder, statementGroup);

        StatementUpdate statementUpdate = builder.build();
        assertEquals(statementUpdate.getAdded(), Collections.singletonList(statement));
        assertEquals(statementUpdate.getReplaced(), Collections.emptyMap());
        assertEquals(statementUpdate.getRemoved(), Collections.emptySet());
    }

    @Test
    public void testAddOrMergeStatementsMatching() {
        when(merger.match(statement1, statement)).thenReturn(true);
        when(merger.match(statement2, statement)).thenReturn(false);
        when(merger.merge(statement1, statement)).thenReturn(statement1);
        StatementEdit statementEdit = new StatementEdit(statement, merger, StatementEditingMode.ADD_OR_MERGE);
        StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

        SUT.contributeToStatementUpdate(builder, statementGroup);

        StatementUpdate statementUpdate = builder.build();
        assertEquals(statementUpdate.getAdded(), Collections.emptyList());
        assertEquals(statementUpdate.getReplaced(), Collections.singletonMap(statementId1, statement1));
        assertEquals(statementUpdate.getRemoved(), Collections.emptySet());
    }

    @Test
    public void testAddOrMergeStatementsAllMatching() {
        when(merger.match(statement1, statement)).thenReturn(true);
        when(merger.match(statement2, statement)).thenReturn(true);
        when(merger.merge(statement1, statement)).thenReturn(statement1);
        StatementEdit statementEdit = new StatementEdit(statement, merger, StatementEditingMode.ADD_OR_MERGE);
        StatementGroupEdit SUT = new StatementGroupEdit(Collections.singletonList(statementEdit));

        SUT.contributeToStatementUpdate(builder, statementGroup);

        StatementUpdate statementUpdate = builder.build();
        assertEquals(statementUpdate.getAdded(), Collections.emptyList());
        assertEquals(statementUpdate.getReplaced(), Collections.singletonMap(statementId1, statement1));
        assertEquals(statementUpdate.getRemoved(), Collections.emptySet());
    }

    @Test
    public void testGetters() {
        assertEquals(SUT.getProperty(), statement.getMainSnak().getPropertyId());
        assertEquals(SUT.getStatementEdits(), Collections.singletonList(statementEdit));
    }

    @Test
    public void testEquality() {
        assertEquals(SUT, new StatementGroupEdit(Collections.singletonList(statementEdit)));
        assertNotEquals(SUT, statementEdit);
    }

    @Test
    public void testHashCode() {
        assertEquals(SUT.hashCode(), new StatementGroupEdit(Collections.singletonList(statementEdit)).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(SUT.toString(), "[" + statementEdit + "]");
    }

    @Test
    public void testJsonSerialization() throws IOException {
        TestUtils.isSerializedTo(SUT, TestingData.jsonFromFile("updates/statement_group_update.json"));
    }
}
