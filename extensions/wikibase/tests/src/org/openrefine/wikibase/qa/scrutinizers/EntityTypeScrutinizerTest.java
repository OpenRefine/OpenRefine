
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
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

    public static String ALLOWED_ENTITY_TYPES_QID = "Q52004125";
    public static String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";
    public static String WIKIBASE_ITEM_QID = "Q29934200";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
    public static Value propertyValue = Datamodel.makeWikidataItemIdValue("Q36322");

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(ALLOWED_ENTITY_TYPES_QID);
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ITEM_OF_PROPERTY_CONSTRAINT_PID);
    public static Value itemValue = Datamodel.makeWikidataItemIdValue("Q29934218");
    public static Value allowedValue = Datamodel.makeWikidataItemIdValue(WIKIBASE_ITEM_QID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new EntityTypeScrutinizer();
    }

    @Test
    public void testAllowed() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak mainValueSnak = Datamodel.makeValueSnak(propertyIdValue, propertyValue);
        Statement statement = new StatementImpl("P2302", mainValueSnak, idA);

        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedValue);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_ENTITY_TYPES_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testDisallowed() {
        ItemIdValue idA = TestingData.existingId;

        ValueSnak mainValueSnak = Datamodel.makeValueSnak(propertyIdValue, propertyValue);
        Statement statement = new StatementImpl("P2302", mainValueSnak, idA);

        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, itemValue);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_ENTITY_TYPES_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(EntityTypeScrutinizer.type);
    }
}
