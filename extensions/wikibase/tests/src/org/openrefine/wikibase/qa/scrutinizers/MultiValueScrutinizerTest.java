
package org.openrefine.wikibase.qa.scrutinizers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public class MultiValueScrutinizerTest extends ScrutinizerTest {

    public static final String MULTI_VALUE_CONSTRAINT_QID = "Q21510857";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P1963");
    public static Value valueSnak = Datamodel.makeWikidataItemIdValue("Q5");
    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(MULTI_VALUE_CONSTRAINT_QID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new MultiValueScrutinizer();
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        ItemIdValue idB = TestingData.matchedId;
        Snak snakValue1 = Datamodel.makeSomeValueSnak(propertyIdValue);
        Snak snakValue2 = Datamodel.makeSomeValueSnak(propertyIdValue);
        Statement statement1 = new StatementImpl("P1963", snakValue1, idA);
        Statement statement2 = new StatementImpl("P1963", snakValue2, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(TestingData.generateStatement(idA, idB)))
                .addStatement(add(TestingData.generateStatement(idA, idB)))
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, MULTI_VALUE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testNewItemTrigger() {
        ItemIdValue idA = TestingData.newIdA;
        ItemIdValue idB = TestingData.newIdB;
        Snak mainSnakValue = Datamodel.makeValueSnak(propertyIdValue, valueSnak);
        Statement statement = new StatementImpl("P1963", mainSnakValue, idA);
        ItemEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(TestingData.generateStatement(idA, idB)))
                .addStatement(add(statement))
                .build();
        ItemEdit updateB = new ItemEditBuilder(idB)
                .addStatement(add(TestingData.generateStatement(idB, idB)))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, MULTI_VALUE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA, updateB);
        assertWarningsRaised(MultiValueScrutinizer.new_type);
    }

    @Test
    public void testExistingItemTrigger() {
        ItemIdValue idA = TestingData.existingId;
        ItemIdValue idB = TestingData.matchedId;
        Snak mainSnakValue = Datamodel.makeValueSnak(propertyIdValue, valueSnak);
        Statement statement = new StatementImpl("P1963", mainSnakValue, idA);
        ItemEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(TestingData.generateStatement(idA, idB)))
                .addStatement(add(statement)).build();
        ItemEdit updateB = new ItemEditBuilder(idB)
                .addStatement(add(TestingData.generateStatement(idB, idB)))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, MULTI_VALUE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA, updateB);
        assertWarningsRaised(MultiValueScrutinizer.existing_type);
    }

}
