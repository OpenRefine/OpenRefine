package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class UseAsQualifierScrutinizerTest extends ScrutinizerTest {
    @Override
    public EditScrutinizer getScrutinizer() {
        return new UseAsQualifierScrutinizer();
    }

    @Test
    public void testTrigger() {
//       assertWarningsRaised(UseAsQualifierScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
//        Snak qualifierSnak = Datamodel.makeValueSnak(itemParameterPID, allowedValue);
//        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
//        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedValueEntity, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        assertNoWarningRaised();
    }
}
