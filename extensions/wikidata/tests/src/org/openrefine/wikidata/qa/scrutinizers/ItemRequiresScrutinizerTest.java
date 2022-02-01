
package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdate;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemRequiresScrutinizerTest extends ScrutinizerTest {

    public static final String ITEM_REQUIRES_CONSTRAINT_QID = "Q21503247";
    public static final String ITEM_REQUIRES_PROPERTY_PID = "P2306";
    public static final String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P157");
    public static ItemIdValue itemValue = Datamodel.makeWikidataItemIdValue("Q3187975");
    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(ITEM_REQUIRES_CONSTRAINT_QID);
    public static PropertyIdValue propertyParameterPID = Datamodel.makeWikidataPropertyIdValue(ITEM_REQUIRES_PROPERTY_PID);
    public static PropertyIdValue propertyParameterValue = Datamodel.makeWikidataPropertyIdValue("P1196");
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ITEM_OF_PROPERTY_CONSTRAINT_PID);
    public static Value requiredValue = Datamodel.makeWikidataItemIdValue("Q149086");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new ItemRequiresScrutinizer();
    }

    @Test
    public void testExistingItemTrigger() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, itemValue);
        Statement statement = new StatementImpl("P157", mainSnak, idA);
        TermedStatementEntityUpdate updateA = new TermedStatementEntityUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, requiredValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ITEM_REQUIRES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA);
        assertWarningsRaised(ItemRequiresScrutinizer.existingItemRequirePropertyType);
    }

    @Test
    public void testWrongValue() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, itemValue);
        Statement statement = new StatementImpl("P157", mainSnak, idA);
        Snak requiredPropertySnak = Datamodel.makeValueSnak(propertyParameterValue, itemValue);
        Statement requiredStatement = new StatementImpl("P1196", requiredPropertySnak, idA);
        TermedStatementEntityUpdate updateA = new TermedStatementEntityUpdateBuilder(idA).addStatement(statement)
                .addStatement(requiredStatement).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, requiredValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ITEM_REQUIRES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA);
        assertWarningsRaised(ItemRequiresScrutinizer.existingItemRequireValuesType);
    }

    @Test
    public void testCorrectValue() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, itemValue);
        Statement statement = new StatementImpl("P157", mainSnak, idA);
        Snak requiredPropertySnak = Datamodel.makeValueSnak(propertyParameterValue, requiredValue);
        Statement requiredStatement = new StatementImpl("P1196", requiredPropertySnak, idA);
        TermedStatementEntityUpdate updateA = new TermedStatementEntityUpdateBuilder(idA).addStatement(statement)
                .addStatement(requiredStatement).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, requiredValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ITEM_REQUIRES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA);
        assertNoWarningRaised();
    }

    @Test
    public void testNewItemTrigger() {
        ItemIdValue idA = TestingData.newIdA;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, itemValue);
        Statement statement = new StatementImpl("P157", mainSnak, idA);
        TermedStatementEntityUpdate updateA = new TermedStatementEntityUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, requiredValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ITEM_REQUIRES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA);
        assertWarningsRaised(ItemRequiresScrutinizer.newItemRequirePropertyType);
    }

    @Test
    public void testExistingItemNoIssue() {
        ItemIdValue id = TestingData.existingId;
        ValueSnak mainSnak1 = Datamodel.makeValueSnak(propertyIdValue, itemValue);
        ValueSnak mainSnak2 = Datamodel.makeValueSnak(propertyParameterValue, requiredValue);
        Statement statement1 = new StatementImpl("P157", mainSnak1, id);
        Statement statement2 = new StatementImpl("P1196", mainSnak2, id);
        TermedStatementEntityUpdate update = new TermedStatementEntityUpdateBuilder(id).addStatement(statement1).addStatement(statement2)
                .build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, requiredValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ITEM_REQUIRES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);

        setFetcher(fetcher);
        scrutinize(update);
        assertNoWarningRaised();
    }
}
