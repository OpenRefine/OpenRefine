
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.NoValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConflictsWithScrutinizerTest extends ScrutinizerTest {

    public static final String CONFLICTS_WITH_CONSTRAINT_QID = "Q21502838";
    public static final String CONFLICTS_WITH_PROPERTY_PID = "P2306";
    public static final String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";

    public static PropertyIdValue conflictsWithPid = Datamodel.makeWikidataPropertyIdValue("P2002");
    public static Value conflictsWithValue = Datamodel.makeWikidataItemIdValue("Q36322");
    public static PropertyIdValue propertyWithConflictsPid1 = Datamodel.makeWikidataPropertyIdValue("P31");
    public static Value conflictingValue1 = Datamodel.makeWikidataItemIdValue("Q4167836");
    public static PropertyIdValue propertyWithConflictsPid2 = Datamodel.makeWikidataPropertyIdValue("P553");
    public static Value conflictingValue2 = Datamodel.makeWikidataItemIdValue("Q918");

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(CONFLICTS_WITH_CONSTRAINT_QID);
    public static PropertyIdValue propertyParameterPID = Datamodel.makeWikidataPropertyIdValue(CONFLICTS_WITH_PROPERTY_PID);
    public static Value conflictingPropertyValue1 = Datamodel.makeWikidataPropertyIdValue("P31");
    public static Value conflictingPropertyValue2 = Datamodel.makeWikidataPropertyIdValue("P553");
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ITEM_OF_PROPERTY_CONSTRAINT_PID);
    public static Value conflictingItemValue1 = Datamodel.makeWikidataItemIdValue("Q4167836");
    public static Value conflictingItemValue2 = Datamodel.makeWikidataItemIdValue("Q918");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new ConflictsWithScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak value1 = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);
        ValueSnak value2 = Datamodel.makeValueSnak(propertyWithConflictsPid1, conflictingValue1);

        Statement statement1 = new StatementImpl("P2002", value1, idA);
        Statement statement2 = new StatementImpl("P31", value2, idA);

        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        Snak snak1 = Datamodel.makeValueSnak(propertyParameterPID, conflictingPropertyValue1);
        Snak snak2 = Datamodel.makeValueSnak(itemParameterPID, conflictingItemValue1);
        List<Snak> snakList1 = Collections.singletonList(snak1);
        List<Snak> snakList2 = Collections.singletonList(snak2);
        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(snakList1);
        SnakGroup snakGroup2 = Datamodel.makeSnakGroup(snakList2);
        List<SnakGroup> constraintQualifiers = Arrays.asList(snakGroup1, snakGroup2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(conflictsWithPid, CONFLICTS_WITH_CONSTRAINT_QID)).thenReturn(constraintDefinitions);

        setFetcher(fetcher);
        scrutinize(updateA);
        assertWarningsRaised(ConflictsWithScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue id = TestingData.existingId;

        ValueSnak value = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);

        Statement statement = new StatementImpl("P2002", value, id);

        TermedStatementEntityEdit update = new ItemEditBuilder(id)
                .addStatement(add(statement))
                .build();

        Snak snak1 = Datamodel.makeValueSnak(propertyParameterPID, conflictingPropertyValue1);
        Snak snak2 = Datamodel.makeValueSnak(itemParameterPID, conflictingItemValue1);
        List<Snak> snakList1 = Collections.singletonList(snak1);
        List<Snak> snakList2 = Collections.singletonList(snak2);
        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(snakList1);
        SnakGroup snakGroup2 = Datamodel.makeSnakGroup(snakList2);
        List<SnakGroup> constraintQualifiers = Arrays.asList(snakGroup1, snakGroup2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(conflictsWithPid, CONFLICTS_WITH_CONSTRAINT_QID)).thenReturn(constraintDefinitions);

        setFetcher(fetcher);
        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testNoValueSnak() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak value1 = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);
        NoValueSnak value2 = Datamodel.makeNoValueSnak(propertyWithConflictsPid1);

        Statement statement1 = new StatementImpl("P2002", value1, idA);
        Statement statement2 = new StatementImpl("P31", value2, idA);

        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        Snak snak1 = Datamodel.makeValueSnak(propertyParameterPID, conflictingPropertyValue1);
        Snak snak2 = Datamodel.makeValueSnak(itemParameterPID, conflictingItemValue1);
        List<Snak> snakList1 = Collections.singletonList(snak1);
        List<Snak> snakList2 = Collections.singletonList(snak2);
        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(snakList1);
        SnakGroup snakGroup2 = Datamodel.makeSnakGroup(snakList2);
        List<SnakGroup> constraintQualifiers = Arrays.asList(snakGroup1, snakGroup2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(conflictsWithPid, CONFLICTS_WITH_CONSTRAINT_QID)).thenReturn(constraintDefinitions);

        setFetcher(fetcher);
        scrutinize(updateA);
        assertNoWarningRaised();
    }

    @Test
    public void testNoStatement() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak valueSnak = Datamodel.makeValueSnak(propertyWithConflictsPid1, conflictingValue1);

        Statement statement = new StatementImpl("P31", valueSnak, idA);

        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = new ArrayList<>();

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyWithConflictsPid1, CONFLICTS_WITH_CONSTRAINT_QID)).thenReturn(constraintDefinitions);

        setFetcher(fetcher);
        scrutinize(updateA);
        assertNoWarningRaised();
    }

    @Test
    public void testMultipleConstraints() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak value1 = Datamodel.makeValueSnak(conflictsWithPid, conflictsWithValue);
        ValueSnak value2 = Datamodel.makeValueSnak(propertyWithConflictsPid1, conflictingValue1);
        ValueSnak value3 = Datamodel.makeValueSnak(propertyWithConflictsPid2, conflictingValue2);

        Statement statement1 = new StatementImpl("P2002", value1, idA);
        Statement statement2 = new StatementImpl("P31", value2, idA);
        Statement statement3 = new StatementImpl("P553", value3, idA);

        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .addStatement(add(statement3))
                .build();

        Snak propertySnak1 = Datamodel.makeValueSnak(propertyParameterPID, conflictingPropertyValue1);
        Snak itemSnak1 = Datamodel.makeValueSnak(itemParameterPID, conflictingItemValue1);
        Snak propertySnak2 = Datamodel.makeValueSnak(propertyParameterPID, conflictingPropertyValue2);
        Snak itemSnak2 = Datamodel.makeValueSnak(itemParameterPID, conflictingItemValue2);
        List<Snak> snakList1 = Arrays.asList(propertySnak1, propertySnak2);
        List<Snak> snakList2 = Arrays.asList(itemSnak1, itemSnak2);
        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(snakList1);
        SnakGroup snakGroup2 = Datamodel.makeSnakGroup(snakList2);
        List<SnakGroup> constraintQualifiers = Arrays.asList(snakGroup1, snakGroup2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(conflictsWithPid, CONFLICTS_WITH_CONSTRAINT_QID)).thenReturn(constraintDefinitions);

        setFetcher(fetcher);
        scrutinize(updateA);
        assertWarningsRaised(ConflictsWithScrutinizer.type, ConflictsWithScrutinizer.type);
    }

}
