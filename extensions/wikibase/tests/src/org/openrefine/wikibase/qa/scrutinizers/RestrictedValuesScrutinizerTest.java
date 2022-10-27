
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
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

public class RestrictedValuesScrutinizerTest extends SnakScrutinizerTest {

    public static final String ALLOWED_VALUES_CONSTRAINT_QID = "Q21510859";
    public static final String DISALLOWED_VALUES_CONSTRAINT_QID = "Q52558054";
    public static final String ALLOWED_VALUES_CONSTRAINT_PID = "P2305";

    private ItemIdValue qid = Datamodel.makeWikidataItemIdValue("Q3487");
    public static PropertyIdValue allowedPropertyIdValue = Datamodel.makeWikidataPropertyIdValue("P1622");
    public static ItemIdValue allowedValue = Datamodel.makeWikidataItemIdValue("Q13196750");
    public static PropertyIdValue disallowedPropertyIdValue = Datamodel.makeWikidataPropertyIdValue("P31");
    public static ItemIdValue disallowedValue = Datamodel.makeWikidataItemIdValue("Q47");
    public static ItemIdValue allowedValueEntity = Datamodel.makeWikidataItemIdValue(ALLOWED_VALUES_CONSTRAINT_QID);
    public static ItemIdValue disallowedValueEntity = Datamodel.makeWikidataItemIdValue(DISALLOWED_VALUES_CONSTRAINT_QID);

    public static PropertyIdValue itemParameterPID = Datamodel.makeWikidataPropertyIdValue(ALLOWED_VALUES_CONSTRAINT_PID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new RestrictedValuesScrutinizer();
    }

    @Test
    public void testNoConstraint() {
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(any(), eq(ALLOWED_VALUES_CONSTRAINT_QID))).thenReturn(new ArrayList<>());
        when(fetcher.getConstraintsByType(any(), eq(DISALLOWED_VALUES_CONSTRAINT_QID))).thenReturn(new ArrayList<>());
        setFetcher(fetcher);
        scrutinize(TestingData.generateStatement(qid,
                Datamodel.makeWikidataPropertyIdValue("P28732"),
                qid));
        assertNoWarningRaised();
    }

    @Test
    public void testAllowedValue() {
        Snak mainSnak = Datamodel.makeValueSnak(allowedPropertyIdValue, allowedValue);
        Statement statement = new StatementImpl("P1622", mainSnak, qid);

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedValueEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(allowedPropertyIdValue, ALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        when(fetcher.getConstraintsByType(allowedPropertyIdValue, DISALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        setFetcher(fetcher);

        scrutinize(statement);
        assertNoWarningRaised();
    }

    @Test
    public void testAllowedValueFailing() {
        Snak mainSnak = Datamodel.makeSomeValueSnak(allowedPropertyIdValue);
        Statement statement = new StatementImpl("P1622", mainSnak, qid);

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedValueEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(allowedPropertyIdValue, ALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        when(fetcher.getConstraintsByType(allowedPropertyIdValue, DISALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        setFetcher(fetcher);

        scrutinize(statement);
        assertWarningsRaised(RestrictedValuesScrutinizer.type);
    }

    @Test
    public void testDisallowedValue() {
        Snak mainSnak = Datamodel.makeSomeValueSnak(disallowedPropertyIdValue);
        Statement statement = new StatementImpl("P31", mainSnak, qid);

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, disallowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(disallowedValueEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(disallowedPropertyIdValue, ALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        when(fetcher.getConstraintsByType(disallowedPropertyIdValue, DISALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(statement);
        assertNoWarningRaised();
    }

    @Test
    public void testDisallowedValueFailing() {
        Snak mainSnak = Datamodel.makeValueSnak(disallowedPropertyIdValue, disallowedValue);
        Statement statement = new StatementImpl("P31", mainSnak, qid);

        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, disallowedValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(disallowedValueEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(disallowedPropertyIdValue, ALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        when(fetcher.getConstraintsByType(disallowedPropertyIdValue, DISALLOWED_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(statement);
        assertWarningsRaised(RestrictedValuesScrutinizer.type);
    }

}
