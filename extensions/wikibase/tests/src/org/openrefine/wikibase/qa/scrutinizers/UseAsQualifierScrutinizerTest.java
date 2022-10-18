
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UseAsQualifierScrutinizerTest extends ScrutinizerTest {

    public static final String ONE_OF_QUALIFIER_VALUE_PROPERTY_QID = "Q52712340";
    public static final String QUALIFIER_PROPERTY_PID = "P2306";
    public static final String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ITEM_OF_PROPERTY_CONSTRAINT_PID);
    public static PropertyIdValue qualifierPID = Datamodel.makeWikidataPropertyIdValue(QUALIFIER_PROPERTY_PID);
    public static PropertyIdValue qualifierPropertyValue = Datamodel.makeWikidataPropertyIdValue("P348");
    public static ItemIdValue qualifierAllowedValue = Datamodel.makeWikidataItemIdValue("Q546");
    public static ItemIdValue qualifierDisallowedValue = Datamodel.makeWikidataItemIdValue("Q12333");
    public static ItemIdValue useAsQualifierEntityId = Datamodel.makeWikidataItemIdValue("Q52712340");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new UseAsQualifierScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue id = TestingData.existingId;
        Snak statementQualifier = Datamodel.makeValueSnak(qualifierPropertyValue, qualifierDisallowedValue);
        List<SnakGroup> qualifierList = makeSnakGroupList(statementQualifier);
        List<Statement> statementList = constraintParameterStatementList(useAsQualifierEntityId, qualifierList);
        Statement statement = statementList.get(0);
        TermedStatementEntityEdit update = new ItemEditBuilder(id).addStatement(add(statement)).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(qualifierPID, qualifierPropertyValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, qualifierAllowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(useAsQualifierEntityId, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(any(), eq(ONE_OF_QUALIFIER_VALUE_PROPERTY_QID))).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(UseAsQualifierScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue id = TestingData.existingId;
        Snak statementQualifier = Datamodel.makeValueSnak(qualifierPropertyValue, qualifierAllowedValue);
        List<SnakGroup> qualifierList = makeSnakGroupList(statementQualifier);
        List<Statement> statementList = constraintParameterStatementList(useAsQualifierEntityId, qualifierList);
        Statement statement = statementList.get(0);
        TermedStatementEntityEdit update = new ItemEditBuilder(id).addStatement(add(statement)).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(qualifierPID, qualifierPropertyValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, qualifierAllowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(useAsQualifierEntityId, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(any(), eq(ONE_OF_QUALIFIER_VALUE_PROPERTY_QID))).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testNoQualifier() {
        ItemIdValue id = TestingData.existingId;
        List<Statement> statementList = constraintParameterStatementList(useAsQualifierEntityId, new ArrayList<>());
        Statement statement = statementList.get(0);
        TermedStatementEntityEdit update = new ItemEditBuilder(id).addStatement(add(statement)).build();

        Snak qualifierSnak1 = Datamodel.makeValueSnak(qualifierPID, qualifierPropertyValue);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(itemParameterPID, qualifierAllowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak1, qualifierSnak2);
        List<Statement> constraintDefinitions = constraintParameterStatementList(useAsQualifierEntityId, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(any(), eq(ONE_OF_QUALIFIER_VALUE_PROPERTY_QID))).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }
}
