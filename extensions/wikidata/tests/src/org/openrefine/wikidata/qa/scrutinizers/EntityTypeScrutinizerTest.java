package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityTypeScrutinizerTest extends StatementScrutinizerTest {

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
    public static Value propertyValue = MockConstraintFetcher.conflictsWithStatementValue;

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue("Q52004125");
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue("P2305");
    public static Value itemValue = Datamodel.makeWikidataItemIdValue("Q29934218");
    public static Value allowedValue = Datamodel.makeWikidataItemIdValue("Q29934200");



    @Override
    public EditScrutinizer getScrutinizer() {
        return new EntityTypeScrutinizer();
    }
    
    @Test
    public void testAllowed() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak mainValueSnak = Datamodel.makeValueSnak(propertyIdValue, propertyValue);
        Statement statement = new StatementImpl("P2302", mainValueSnak, idA);

        ItemUpdate update = new ItemUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, itemValue);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> snakGroupList = Collections.singletonList(qualifierSnakGroup);
        List<Statement> statementList = constraintParameterStatementList(entityIdValue, snakGroupList);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue,"Q52004125")).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, "P2305")).thenReturn(Collections.singletonList(allowedValue));
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testDisallowed() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak mainValueSnak = Datamodel.makeValueSnak(propertyIdValue, propertyValue);
        Statement statement = new StatementImpl("P2302", mainValueSnak, idA);

        ItemUpdate update = new ItemUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, itemValue);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> snakGroupList = Collections.singletonList(qualifierSnakGroup);
        List<Statement> statementList = constraintParameterStatementList(entityIdValue, snakGroupList);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue,"Q52004125")).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, "P2305")).thenReturn(Collections.singletonList(itemValue));
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(EntityTypeScrutinizer.type);
    }
}
