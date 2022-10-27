
package org.openrefine.wikibase.schema.entityvalues;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.FormIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.LexemeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.SenseIdValue;

public class SuggestedEntityIdValueTest {

    @Test
    public void testSuggestedEntityIdValueType() {
        SuggestedEntityIdValue value = SuggestedEntityIdValue.build("M1234", "http://foo.com/bar/", "my file");
        Assert.assertTrue(value instanceof MediaInfoIdValue);
        value = SuggestedEntityIdValue.build("P1234", "http://foo.com/bar/", "my file");
        Assert.assertTrue(value instanceof PropertyIdValue);
        value = SuggestedEntityIdValue.build("Q1234", "http://foo.com/bar/", "my file");
        Assert.assertTrue(value instanceof ItemIdValue);
        value = SuggestedEntityIdValue.build("L1234", "http://foo.com/bar/", "my file");
        Assert.assertTrue(value instanceof LexemeIdValue);
        value = SuggestedEntityIdValue.build("L1234-S123", "http://foo.com/bar/", "my file");
        Assert.assertTrue(value instanceof SenseIdValue);
        value = SuggestedEntityIdValue.build("L1234-F123", "http://foo.com/bar/", "my file");
        Assert.assertTrue(value instanceof FormIdValue);
    }
}
