/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.qa;

import java.util.regex.Pattern;

import org.openrefine.wikidata.utils.EntityCacheStub;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class WikidataConstraintFetcherTests {

    private ConstraintFetcher fetcher;

    private PropertyIdValue headOfGovernment;
    private PropertyIdValue startTime;
    private PropertyIdValue endTime;
    private PropertyIdValue instanceOf;
    private PropertyIdValue gridId;
    private PropertyIdValue partOf;
    private PropertyIdValue mother;
    private PropertyIdValue child;

    public WikidataConstraintFetcherTests() {
        fetcher = new WikidataConstraintFetcher(new EntityCacheStub());
        headOfGovernment = Datamodel.makeWikidataPropertyIdValue("P6");
        startTime = Datamodel.makeWikidataPropertyIdValue("P580");
        endTime = Datamodel.makeWikidataPropertyIdValue("P582");
        instanceOf = Datamodel.makeWikidataPropertyIdValue("P31");
        gridId = Datamodel.makeWikidataPropertyIdValue("P2427");
        partOf = Datamodel.makeWikidataPropertyIdValue("P361");
        mother = Datamodel.makeWikidataPropertyIdValue("P25");
        child = Datamodel.makeWikidataPropertyIdValue("P40");
    }

    @Test
    public void testGetFormatConstraint() {
        String regex = fetcher.getFormatRegex(gridId);
        Pattern pattern = Pattern.compile(regex);

        Assert.assertTrue(pattern.matcher("grid.470811.b").matches());
        Assert.assertFalse(pattern.matcher("501100006367").matches());

        Assert.assertNull(fetcher.getFormatRegex(instanceOf));
    }

    @Test
    public void testGetInverseConstraint() {
        Assert.assertEquals(fetcher.getInversePid(mother), child);
    }

    @Test
    public void testAllowedQualifiers() {
        Assert.assertTrue(fetcher.allowedQualifiers(headOfGovernment).contains(startTime));
        Assert.assertTrue(fetcher.allowedQualifiers(headOfGovernment).contains(endTime));
        Assert.assertFalse(fetcher.allowedQualifiers(headOfGovernment).contains(headOfGovernment));
        Assert.assertNull(fetcher.allowedQualifiers(startTime));
    }

    @Test
    public void testMandatoryQualifiers() {
        Assert.assertTrue(fetcher.mandatoryQualifiers(headOfGovernment).contains(startTime));
        Assert.assertFalse(fetcher.mandatoryQualifiers(headOfGovernment).contains(endTime));
        Assert.assertNull(fetcher.allowedQualifiers(startTime));
    }

    @Test
    public void testSingleValue() {
        Assert.assertFalse(fetcher.hasSingleValue(headOfGovernment));
        Assert.assertTrue(fetcher.hasSingleValue(mother));
    }

    @Test
    public void testDistinctValues() {
        Assert.assertFalse(fetcher.hasDistinctValues(partOf));
        Assert.assertTrue(fetcher.hasDistinctValues(gridId));
    }
}
