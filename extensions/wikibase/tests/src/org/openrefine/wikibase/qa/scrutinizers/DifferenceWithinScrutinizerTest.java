
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.implementation.TimeValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DifferenceWithinScrutinizerTest extends ScrutinizerTest {

    public static String DIFFERENCE_WITHIN_RANGE_CONSTRAINT_QID = "Q21510854";
    public static String DIFFERENCE_WITHIN_RANGE_CONSTRAINT_PID = "P2306";
    public static String MINIMUM_VALUE_PID = "P2313";
    public static String MAXIMUM_VALUE_PID = "P2312";

    public static PropertyIdValue upperBoundPid = Datamodel.makeWikidataPropertyIdValue("P570");
    public static PropertyIdValue lowerBoundPid = Datamodel.makeWikidataPropertyIdValue("P569");
    public static QuantityValue minValue = Datamodel.makeQuantityValue(new BigDecimal(0));
    public static QuantityValue maxValue = Datamodel.makeQuantityValue(new BigDecimal(150));
    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(DIFFERENCE_WITHIN_RANGE_CONSTRAINT_QID);

    public static PropertyIdValue propertyParameterPID = Datamodel.makeWikidataPropertyIdValue(DIFFERENCE_WITHIN_RANGE_CONSTRAINT_PID);
    public static PropertyIdValue minimumValuePID = Datamodel.makeWikidataPropertyIdValue(MINIMUM_VALUE_PID);
    public static PropertyIdValue maximumValuePID = Datamodel.makeWikidataPropertyIdValue(MAXIMUM_VALUE_PID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new DifferenceWithinRangeScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;
        TimeValue lowerYear = new TimeValueImpl(1800, (byte) 10, (byte) 15, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO);
        TimeValue upperYear = new TimeValueImpl(2020, (byte) 10, (byte) 15, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO);
        ValueSnak value1 = Datamodel.makeValueSnak(lowerBoundPid, lowerYear);
        ValueSnak value2 = Datamodel.makeValueSnak(upperBoundPid, upperYear);
        Statement statement1 = new StatementImpl("P569", value1, idA);
        Statement statement2 = new StatementImpl("P570", value2, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        Snak propertyQualifier = Datamodel.makeValueSnak(propertyParameterPID, lowerBoundPid);
        Snak minValueQualifier = Datamodel.makeValueSnak(minimumValuePID, minValue);
        Snak maxValueQualifier = Datamodel.makeValueSnak(maximumValuePID, maxValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(propertyQualifier, minValueQualifier, maxValueQualifier);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(upperBoundPid, DIFFERENCE_WITHIN_RANGE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA);
        assertWarningsRaised(DifferenceWithinRangeScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        TimeValue lowerYear = new TimeValueImpl(2000, (byte) 10, (byte) 15, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO);
        TimeValue upperYear = new TimeValueImpl(2020, (byte) 10, (byte) 15, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO);
        ValueSnak value1 = Datamodel.makeValueSnak(lowerBoundPid, lowerYear);
        ValueSnak value2 = Datamodel.makeValueSnak(upperBoundPid, upperYear);
        Statement statement1 = new StatementImpl("P569", value1, idA);
        Statement statement2 = new StatementImpl("P570", value2, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        Snak propertyQualifier = Datamodel.makeValueSnak(propertyParameterPID, lowerBoundPid);
        Snak minValueQualifier = Datamodel.makeValueSnak(minimumValuePID, minValue);
        Snak maxValueQualifier = Datamodel.makeValueSnak(maximumValuePID, maxValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(propertyQualifier, minValueQualifier, maxValueQualifier);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(upperBoundPid, DIFFERENCE_WITHIN_RANGE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(updateA);
        assertNoWarningRaised();
    }
}
