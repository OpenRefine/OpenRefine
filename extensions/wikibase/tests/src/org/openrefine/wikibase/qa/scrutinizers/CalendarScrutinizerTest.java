
package org.openrefine.wikibase.qa.scrutinizers;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

public class CalendarScrutinizerTest extends ValueScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new CalendarScrutinizer();
    }

    @Test
    public void testScrutinizeRecentValue() {
        scrutinize(Datamodel.makeTimeValue(1978L, (byte) 3, (byte) 4, (byte) 0, (byte) 0, (byte) 0, 11, TimeValue.CM_GREGORIAN_PRO));
        assertNoWarningRaised();
    }

    @Test
    public void testScrutinizeCloseValue() {
        scrutinize(Datamodel.makeTimeValue(1582L, (byte) 10, (byte) 17, (byte) 0, (byte) 0, (byte) 0, 11, TimeValue.CM_GREGORIAN_PRO));
        assertNoWarningRaised();
    }

    @Test
    public void testScrutinizeEarlyYear() {
        scrutinize(Datamodel.makeTimeValue(1400L, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 9, 0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO));
        assertNoWarningRaised();
    }

    @Test
    public void testScrutinizeEarlyDay() {
        scrutinize(Datamodel.makeTimeValue(1440L, (byte) 10, (byte) 17, (byte) 0, (byte) 0, (byte) 0, 11, TimeValue.CM_GREGORIAN_PRO));
        assertWarningsRaised(CalendarScrutinizer.earlyGregorianDateType);
    }
}
