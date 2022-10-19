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

package org.openrefine.wikibase.updates.scheduler;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.UnsupportedValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public class PointerExtractorTest {

    private PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P89");
    private Snak snakWithNew = Datamodel.makeValueSnak(pid, TestingData.newIdA);
    private Snak snakWithoutNew = Datamodel.makeValueSnak(pid, TestingData.matchedId);
    private SnakGroup snakGroupWithNew = Datamodel.makeSnakGroup(Collections.singletonList(snakWithNew));
    private SnakGroup snakGroupWithoutNew = Datamodel.makeSnakGroup(Collections.singletonList(snakWithoutNew));
    private Claim claimWithNew = Datamodel.makeClaim(TestingData.existingId, snakWithNew, Collections.emptyList());
    private Claim claimNewSubject = Datamodel.makeClaim(TestingData.newIdB, snakWithoutNew, Collections.emptyList());
    private Claim claimNewQualifier = Datamodel.makeClaim(TestingData.matchedId, snakWithoutNew,
            Collections.singletonList(snakGroupWithNew));

    private static PointerExtractor e = new PointerExtractor();

    @Test
    public void testExtractEntityId() {
        assertEquals(Collections.singleton(TestingData.newIdA), e.extractPointers(TestingData.newIdA));
        assertEmpty(e.extractPointers(TestingData.existingId));
        assertEmpty(e.extractPointers(TestingData.matchedId));
    }

    @Test
    public void testExtractDatavalues() {
        assertEmpty(Datamodel.makeGlobeCoordinatesValue(1.34, 2.354, 0.1, GlobeCoordinatesValue.GLOBE_EARTH));
        assertEmpty(Datamodel.makeStringValue("est"));
        assertEmpty(Datamodel.makeMonolingualTextValue("srtu", "en"));
        assertEmpty(Datamodel.makeWikidataPropertyIdValue("P78"));
        assertEmpty(Datamodel.makeQuantityValue(new BigDecimal("898")));
        assertEmpty(Datamodel.makeQuantityValue(new BigDecimal("7.87"), Datamodel.makeWikidataItemIdValue("Q34")));
        assertEmpty(Datamodel.makeTimeValue(1898, (byte) 2, (byte) 3, TimeValue.CM_GREGORIAN_PRO));
        assertEmpty(mock(UnsupportedValue.class));
    }

    @Test
    public void testSnak() {
        assertEmpty(e.extractPointers(snakWithoutNew));
        assertEquals(Collections.singleton(TestingData.newIdA), e.extractPointers(snakWithNew));
        assertEmpty(e.extractPointers(Datamodel.makeNoValueSnak(pid)));
    }

    @Test
    public void testSnakGroup() {
        assertEmpty(e.extractPointers(snakGroupWithoutNew));
        assertEquals(Collections.singleton(TestingData.newIdA), e.extractPointers(snakGroupWithNew));
    }

    @Test
    public void testStatement() {
        assertEmpty(e.extractPointers(
                Datamodel.makeStatement(claimNewSubject, Collections.emptyList(), StatementRank.NORMAL, "")));
        assertEquals(Collections.singleton(TestingData.newIdA), e.extractPointers(
                Datamodel.makeStatement(claimWithNew, Collections.emptyList(), StatementRank.NORMAL, "")));
        assertEquals(Collections.singleton(TestingData.newIdA), e.extractPointers(
                Datamodel.makeStatement(claimNewQualifier, Collections.emptyList(), StatementRank.NORMAL, "")));
        Reference reference = Datamodel.makeReference(Collections.singletonList(snakGroupWithNew));
        assertEquals(Collections.singleton(TestingData.newIdA), e.extractPointers(Datamodel
                .makeStatement(claimNewSubject, Collections.singletonList(reference), StatementRank.NORMAL, "")));
    }

    private static void assertEmpty(Value v) {
        assertEmpty(e.extractPointers(v));
    }

    private static void assertEmpty(Set<ReconEntityIdValue> pointers) {
        assertEquals(Collections.emptySet(), pointers);
    }
}
