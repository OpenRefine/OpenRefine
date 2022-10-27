
package org.openrefine.wikibase.exporters;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;

public class QSSnakPrinterTest {

    private QSSnakPrinter refPrinter = new QSSnakPrinter(true);
    private QSSnakPrinter mainPrinter = new QSSnakPrinter(false);
    private PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P123");
    private ItemIdValue qid = Datamodel.makeWikidataItemIdValue("Q456");

    @Test
    public void testReferenceValueSnak() {
        Snak snak = Datamodel.makeValueSnak(pid, qid);
        Assert.assertEquals(snak.accept(refPrinter), "\tS123\tQ456");
    }

    @Test
    public void testMainValueSnak() {
        Snak snak = Datamodel.makeValueSnak(pid, qid);
        Assert.assertEquals(snak.accept(mainPrinter), "\tP123\tQ456");
    }

    @Test
    public void testReferenceNoValueSnak() {
        Snak snak = Datamodel.makeNoValueSnak(pid);
        Assert.assertEquals(snak.accept(refPrinter), "\tS123\tnovalue");
    }

    @Test
    public void testMainSomeValueSnak() {
        Snak snak = Datamodel.makeSomeValueSnak(pid);
        Assert.assertEquals(snak.accept(mainPrinter), "\tP123\tsomevalue");
    }
}
