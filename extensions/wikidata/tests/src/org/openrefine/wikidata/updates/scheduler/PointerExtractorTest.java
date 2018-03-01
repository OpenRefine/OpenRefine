package org.openrefine.wikidata.updates.scheduler;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public class PointerExtractorTest {
    
    private ItemIdValue existingId = Datamodel.makeWikidataItemIdValue("Q43");
    private ItemIdValue matchedId = TestingDataGenerator.makeMatchedItemIdValue("Q89","eist");
    private ItemIdValue newIdA = TestingDataGenerator.makeNewItemIdValue(1234L, "new item A");
    private ItemIdValue newIdB = TestingDataGenerator.makeNewItemIdValue(4567L, "new item B");
    
    private PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P89");
    private Snak snakWithNew = Datamodel.makeValueSnak(pid, newIdA);
    private Snak snakWithoutNew = Datamodel.makeValueSnak(pid, matchedId);
    private SnakGroup snakGroupWithNew = Datamodel.makeSnakGroup(Collections.singletonList(snakWithNew));
    private SnakGroup snakGroupWithoutNew = Datamodel.makeSnakGroup(Collections.singletonList(snakWithoutNew));
    private Claim claimWithNew = Datamodel.makeClaim(existingId, snakWithNew, Collections.emptyList());
    private Claim claimNewSubject = Datamodel.makeClaim(newIdB, snakWithoutNew, Collections.emptyList());
    private Claim claimNewQualifier = Datamodel.makeClaim(matchedId, snakWithoutNew,
            Collections.singletonList(snakGroupWithNew));
    
    private static PointerExtractor e = new PointerExtractor();

    @Test
    public void testExtractEntityId() {
        assertEquals(Collections.singleton(newIdA), e.extractPointers(newIdA));
        assertEmpty(e.extractPointers(existingId));
        assertEmpty(e.extractPointers(matchedId));
    }
    
    @Test
    public void testExtractDatavalues() {
        assertEmpty(Datamodel.makeDatatypeIdValue("string"));
        assertEmpty(Datamodel.makeGlobeCoordinatesValue(1.34, 2.354, 0.1, GlobeCoordinatesValue.GLOBE_EARTH));
        assertEmpty(Datamodel.makeStringValue("est"));
        assertEmpty(Datamodel.makeMonolingualTextValue("srtu", "en"));
        assertEmpty(Datamodel.makeWikidataPropertyIdValue("P78"));
        assertEmpty(Datamodel.makeQuantityValue(new BigDecimal("898")));
        assertEmpty(Datamodel.makeQuantityValue(new BigDecimal("7.87"), "http://www.wikidata.org/entity/Q34"));
        assertEmpty(Datamodel.makeTimeValue(1898, (byte)2, (byte)3, TimeValue.CM_GREGORIAN_PRO));
    }
    
    @Test
    public void testSnak() {
        assertEmpty(e.extractPointers(snakWithoutNew));
        assertEquals(Collections.singleton(newIdA), e.extractPointers(snakWithNew));
        assertEmpty(e.extractPointers(Datamodel.makeNoValueSnak(pid)));
    }
    
    @Test
    public void testSnakGroup() {
        assertEmpty(e.extractPointers(snakGroupWithoutNew));
        assertEquals(Collections.singleton(newIdA), e.extractPointers(snakGroupWithNew));
    }
    
    @Test
    public void testStatement() {
        assertEmpty(e.extractPointers(Datamodel.makeStatement(claimNewSubject,
                Collections.emptyList(), StatementRank.NORMAL, "")));
        assertEquals(Collections.singleton(newIdA), e.extractPointers(Datamodel.makeStatement(claimWithNew,
                Collections.emptyList(), StatementRank.NORMAL, "")));
        assertEquals(Collections.singleton(newIdA), e.extractPointers(Datamodel.makeStatement(claimNewQualifier,
                Collections.emptyList(), StatementRank.NORMAL, "")));
        Reference reference = Datamodel.makeReference(Collections.singletonList(snakGroupWithNew));
        assertEquals(Collections.singleton(newIdA), e.extractPointers(Datamodel.makeStatement(claimNewSubject,
                Collections.singletonList(reference), StatementRank.NORMAL, "")));
    }
    
    private static void assertEmpty(Value v) {
        assertEmpty(e.extractPointers(v));
    }

    private static void assertEmpty(Set<ReconItemIdValue> pointers) {
        assertEquals(Collections.emptySet(), pointers);
    }
}
