package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.implementation.TimeValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.*;

public class DifferenceWithinScrutinizerTest extends ScrutinizerTest{
    @Override
    public EditScrutinizer getScrutinizer() {
        return new DifferenceWithinRangeScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;
        PropertyIdValue lowerBoundPid = MockConstraintFetcher.lowerBoundPid;
        PropertyIdValue upperBoundPid = MockConstraintFetcher.differenceWithinRangePid;

        TimeValue lowerYear = new TimeValueImpl(1800, (byte)10, (byte)15, (byte)0, (byte)0, (byte)0, (byte)11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);
        TimeValue upperYear = new TimeValueImpl(2020, (byte)10, (byte)15, (byte)0, (byte)0, (byte)0, (byte)11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);

        ValueSnak value1 = Datamodel.makeValueSnak(lowerBoundPid, lowerYear);
        ValueSnak value2 = Datamodel.makeValueSnak(upperBoundPid, upperYear);

        Statement statement1 = new StatementImpl("P569", value1,idA);
        Statement statement2 = new StatementImpl("P570", value2,idA);

        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement1).addStatement(statement2).build();
        scrutinize(updateA);
        assertWarningsRaised(DifferenceWithinRangeScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        PropertyIdValue lowerBoundPid = MockConstraintFetcher.lowerBoundPid;
        PropertyIdValue upperBoundPid = MockConstraintFetcher.differenceWithinRangePid;

        TimeValue lowerYear = new TimeValueImpl(2000, (byte)10, (byte)15, (byte)0, (byte)0, (byte)0, (byte)11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);
        TimeValue upperYear = new TimeValueImpl(2020, (byte)10, (byte)15, (byte)0, (byte)0, (byte)0, (byte)11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);

        ValueSnak value1 = Datamodel.makeValueSnak(lowerBoundPid, lowerYear);
        ValueSnak value2 = Datamodel.makeValueSnak(upperBoundPid, upperYear);

        Statement statement1 = new StatementImpl("P569", value1,idA);
        Statement statement2 = new StatementImpl("P570", value2,idA);

        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement1).addStatement(statement2).build();
        scrutinize(updateA);
        assertNoWarningRaised();
    }
}
