package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.interfaces.*;

public class ConflictsWithScrutinizerTest extends ScrutinizerTest {
    @Override
    public EditScrutinizer getScrutinizer() {
        return new ConflictsWithScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;

        PropertyIdValue conflictsWithPid = MockConstraintFetcher.conflictsWithPid;
        Value conflictsWithValue = MockConstraintFetcher.conflictsWithStatementValue;

        PropertyIdValue propertyWithConflictsPid = MockConstraintFetcher.conflictingStatementPid;
        Value conflictingValue  = MockConstraintFetcher.conflictingStatementValue;

        ValueSnak value1 = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);
        ValueSnak value2 = Datamodel.makeValueSnak(propertyWithConflictsPid, conflictingValue);

        Statement statement1 = new StatementImpl("P50", value1,idA);
        Statement statement2 = new StatementImpl("P31", value2,idA);

        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement1).addStatement(statement2).build();

        scrutinize(updateA);
        assertWarningsRaised(ConflictsWithScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;

        PropertyIdValue conflictsWithPid = MockConstraintFetcher.conflictsWithPid;
        Value conflictsWithValue = MockConstraintFetcher.conflictsWithStatementValue;

        ValueSnak value1 = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);

        Statement statement1 = new StatementImpl("P50", value1,idA);

        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement1).build();
        scrutinize(updateA);
        assertNoWarningRaised();
    }

    @Test
    public void testNoValueSnak() {
        ItemIdValue idA = TestingData.existingId;

        PropertyIdValue conflictsWithPid = MockConstraintFetcher.conflictsWithPid;
        Value conflictsWithValue = MockConstraintFetcher.conflictsWithStatementValue;

        PropertyIdValue propertyWithConflictsPid = MockConstraintFetcher.conflictingStatementPid;

        ValueSnak value1 = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);
        NoValueSnak value2 = Datamodel.makeNoValueSnak(propertyWithConflictsPid);

        Statement statement1 = new StatementImpl("P50", value1,idA);
        Statement statement2 = new StatementImpl("P31", value2,idA);

        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement1).addStatement(statement2).build();

        scrutinize(updateA);
        assertNoWarningRaised();
    }

}

