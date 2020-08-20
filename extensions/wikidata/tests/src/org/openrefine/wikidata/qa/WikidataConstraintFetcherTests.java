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

import org.openrefine.wikidata.qa.scrutinizers.ConflictsWithScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.RestrictedValuesScrutinizer;
import org.openrefine.wikidata.utils.EntityCacheStub;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.util.ArrayList;

public class WikidataConstraintFetcherTests {

    private ConstraintFetcher fetcher;
    public static PropertyIdValue instanceOf;

    public WikidataConstraintFetcherTests() {
        fetcher = new WikidataConstraintFetcher(new EntityCacheStub());
        instanceOf = Datamodel.makeWikidataPropertyIdValue("P31");
    }

    @Test
    public void testGetConstraintsByType() {
        Assert.assertEquals(fetcher.getConstraintsByType(instanceOf, ConflictsWithScrutinizer.CONFLICTS_WITH_CONSTRAINT_QID), new ArrayList<>());
        String constraintDefinitions = "[[ID P31$43E28495-355E-451E-A881-2EE14DFBE99D] http://www.wikidata.org/entity/P31 (property): http://www.wikidata.org/entity/P2302 :: http://www.wikidata.org/entity/Q52558054 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q467 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q6581072 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q6581097 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q8441 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q171283 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q11629 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q11634 (item)\n" +
                "      http://www.wikidata.org/entity/P2305 :: http://www.wikidata.org/entity/Q131123 (item)\n" +
                "      http://www.wikidata.org/entity/P2316 :: http://www.wikidata.org/entity/Q21502408 (item)\n" +
                "]";
        Assert.assertEquals(fetcher.getConstraintsByType(instanceOf, RestrictedValuesScrutinizer.DISALLOWED_VALUES_CONSTRAINT_QID).toString(), constraintDefinitions);
    }
}
