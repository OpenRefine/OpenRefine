
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
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuantityScrutinizerTest extends ValueScrutinizerTest {

    public static final String NO_BOUNDS_CONSTRAINT_QID = "Q51723761";
    public static final String INTEGER_VALUED_CONSTRAINT_QID = "Q52848401";
    public static final String ALLOWED_UNITS_CONSTRAINT_QID = "Q21514353";
    public static final String ALLOWED_UNITS_CONSTRAINT_PID = "P2305";

    private QuantityValue exactValue = Datamodel.makeQuantityValue(
            new BigDecimal("1.234"));

    private QuantityValue integerValue = Datamodel.makeQuantityValue(
            new BigDecimal("132"));

    private QuantityValue trailingZeros = Datamodel.makeQuantityValue(
            new BigDecimal("132.00"));

    private QuantityValue valueWithBounds = Datamodel.makeQuantityValue(
            new BigDecimal("1.234"),
            new BigDecimal("1.200"),
            new BigDecimal("1.545"));

    private QuantityValue wrongUnitValue = Datamodel.makeQuantityValue(
            new BigDecimal("1.234"), Datamodel.makeWikidataItemIdValue("Q346721"));

    private QuantityValue goodUnitValue = Datamodel.makeQuantityValue(
            new BigDecimal("1.234"), (ItemIdValue) allowedUnit);

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P1083");
    public static ItemIdValue noBoundsEntity = Datamodel.makeWikidataItemIdValue(NO_BOUNDS_CONSTRAINT_QID);
    public static ItemIdValue integerValueEntity = Datamodel.makeWikidataItemIdValue(INTEGER_VALUED_CONSTRAINT_QID);
    public static ItemIdValue allowedUnitEntity = Datamodel.makeWikidataItemIdValue(ALLOWED_UNITS_CONSTRAINT_QID);
    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ALLOWED_UNITS_CONSTRAINT_PID);
    public static Value allowedUnit = Datamodel.makeWikidataItemIdValue("Q5");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new QuantityScrutinizer();
    }

    @Test
    public void testBoundsAllowed() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, valueWithBounds);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, NO_BOUNDS_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testBoundsDisallowed() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, valueWithBounds);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(noBoundsEntity, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, NO_BOUNDS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QuantityScrutinizer.boundsDisallowedType);
    }

    @Test
    public void testFractionalAllowed() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, exactValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, INTEGER_VALUED_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testFractionalDisallowed() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, exactValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(integerValueEntity, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, INTEGER_VALUED_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QuantityScrutinizer.integerConstraintType);
    }

    @Test
    public void testTrailingZeros() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, trailingZeros);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(integerValueEntity, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, INTEGER_VALUED_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QuantityScrutinizer.integerConstraintType);
    }

    @Test
    public void testInteger() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, integerValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(integerValueEntity, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, INTEGER_VALUED_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testUnitReqired() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, integerValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedUnit);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(snakGroup1);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedUnitEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_UNITS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QuantityScrutinizer.noUnitProvidedType);
    }

    @Test
    public void testWrongUnit() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, wrongUnitValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedUnit);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(snakGroup1);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedUnitEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_UNITS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QuantityScrutinizer.invalidUnitType);
    }

    @Test
    public void testGoodUnit() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, goodUnitValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedUnit);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedUnitEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_UNITS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testUnitForbidden() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, goodUnitValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedUnitEntity, new ArrayList<>());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_UNITS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QuantityScrutinizer.invalidUnitType);
    }

    @Test
    public void testNoUnit() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, integerValue);
        Statement statement = new StatementImpl("P1083", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, ALLOWED_UNITS_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }
}
