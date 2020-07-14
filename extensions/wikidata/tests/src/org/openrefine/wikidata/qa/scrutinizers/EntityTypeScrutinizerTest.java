package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.ConstraintFetcher;
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
import static org.openrefine.wikidata.qa.scrutinizers.EntityTypeScrutinizer.ALLOWED_ENTITY_TYPES_PID;
import static org.openrefine.wikidata.qa.scrutinizers.EntityTypeScrutinizer.ALLOWED_ENTITY_TYPES_QID;
import static org.openrefine.wikidata.qa.scrutinizers.EntityTypeScrutinizer.ALLOWED_ITEM_TYPE_QID;

public class EntityTypeScrutinizerTest extends StatementScrutinizerTest {

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
    public static Value propertyValue = Datamodel.makeWikidataItemIdValue("Q36322");

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(ALLOWED_ENTITY_TYPES_QID);
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ALLOWED_ENTITY_TYPES_PID);
    public static Value itemValue = Datamodel.makeWikidataItemIdValue("Q29934218");
    public static Value allowedValue = Datamodel.makeWikidataItemIdValue(ALLOWED_ITEM_TYPE_QID);



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
        when(fetcher.getConstraintsByType(propertyIdValue,ALLOWED_ENTITY_TYPES_QID)).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, ALLOWED_ENTITY_TYPES_PID)).thenReturn(Collections.singletonList(allowedValue));
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
        when(fetcher.getConstraintsByType(propertyIdValue,ALLOWED_ENTITY_TYPES_QID)).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, ALLOWED_ENTITY_TYPES_PID)).thenReturn(Collections.singletonList(itemValue));
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(EntityTypeScrutinizer.type);
    }
}
