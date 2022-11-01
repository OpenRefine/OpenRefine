
package org.openrefine.wikibase.utils;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public class SnakUtilsTests {

    PropertyIdValue pidA = Datamodel.makeWikidataPropertyIdValue("P1");
    PropertyIdValue pidB = Datamodel.makeWikidataPropertyIdValue("P2");
    PropertyIdValue pidC = Datamodel.makeWikidataPropertyIdValue("P3");
    StringValue value1 = Datamodel.makeStringValue("value1");
    StringValue value2 = Datamodel.makeStringValue("value2");

    Snak snakA1 = Datamodel.makeValueSnak(pidA, value1);
    Snak snakA2 = Datamodel.makeValueSnak(pidA, value2);
    Snak snakB1 = Datamodel.makeValueSnak(pidB, value1);
    Snak snakC1 = Datamodel.makeValueSnak(pidC, value1);
    Snak snakC2 = Datamodel.makeValueSnak(pidC, value2);

    @Test
    public void testGroupSnaks() {
        List<SnakGroup> snakGroups = SnakUtils.groupSnaks(Arrays.asList(snakA2, snakB1, snakC1, snakA1, snakC2));

        List<SnakGroup> expected = Arrays.asList(
                Datamodel.makeSnakGroup(Arrays.asList(snakA2, snakA1)),
                Datamodel.makeSnakGroup(Arrays.asList(snakB1)),
                Datamodel.makeSnakGroup(Arrays.asList(snakC1, snakC2)));

        Assert.assertEquals(snakGroups, expected);
    }
}
