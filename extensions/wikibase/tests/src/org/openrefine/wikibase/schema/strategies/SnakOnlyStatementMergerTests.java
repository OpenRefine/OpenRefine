
package org.openrefine.wikibase.schema.strategies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class SnakOnlyStatementMergerTests {

    SnakOnlyStatementMerger SUT = new SnakOnlyStatementMerger(new StrictValueMatcher());

    PropertyIdValue pid = TestingData.existingPropertyId;
    PropertyIdValue qualifierPid = Datamodel.makeWikidataPropertyIdValue("P4736");
    Value value1 = Datamodel.makeWikidataItemIdValue("Q4894");
    Value value2 = Datamodel.makeWikidataItemIdValue("Q3645");

    Statement statement1 = TestingData.generateStatement(
            TestingData.matchedId,
            pid,
            value1);
    Statement statement2 = TestingData.generateStatement(
            TestingData.matchedId,
            pid,
            value2);
    Statement statement3 = TestingData.generateStatement(
            TestingData.matchedId,
            TestingData.newPropertyIdA,
            value2);

    Statement statement4 = Datamodel.makeStatement(
            Datamodel.makeClaim(TestingData.matchedId, Datamodel.makeNoValueSnak(pid), Collections.emptyList()),
            Collections.emptyList(), StatementRank.NORMAL, "");

    Statement statement5 = Datamodel.makeStatement(
            Datamodel.makeClaim(TestingData.matchedId, Datamodel.makeSomeValueSnak(pid), Collections.emptyList()),
            Collections.emptyList(), StatementRank.NORMAL, "");

    SnakGroup qualifier = Datamodel.makeSnakGroup(Collections.singletonList(Datamodel.makeValueSnak(qualifierPid, value2)));
    Claim claim1 = Datamodel.makeClaim(TestingData.matchedId, Datamodel.makeValueSnak(pid, value1), Collections.singletonList(qualifier));
    Statement statement1WithQualifier = Datamodel.makeStatement(claim1, Collections.emptyList(), StatementRank.NORMAL, "");
    Claim claim2 = Datamodel.makeClaim(TestingData.matchedId, Datamodel.makeValueSnak(pid, value2), Collections.singletonList(qualifier));
    Statement statement2WithQualifier = Datamodel.makeStatement(claim1, Collections.emptyList(), StatementRank.NORMAL, "");

    @Test
    public void testMatchStatements() {
        assertTrue(SUT.match(statement1, statement1));
        assertFalse(SUT.match(statement1, statement2));
        assertFalse(SUT.match(statement2, statement3));
        assertTrue(SUT.match(statement1, statement1WithQualifier));
        assertFalse(SUT.match(statement1, statement4));
        assertFalse(SUT.match(statement1, statement5));
        assertFalse(SUT.match(statement4, statement5));
        assertTrue(SUT.match(statement5, statement5));
    }

    @Test
    public void testMergeStatements() {
        assertEquals(SUT.merge(statement1, statement1), statement1);
        assertEquals(SUT.merge(statement1WithQualifier, statement2), statement2WithQualifier);
    }

    @Test
    public void testEquals() {
        assertNotEquals(SUT, new PropertyOnlyStatementMerger());
        assertEquals(SUT, new SnakOnlyStatementMerger(new StrictValueMatcher()));
        assertNotEquals(SUT, new SnakOnlyStatementMerger(new LaxValueMatcher()));
    }

    @Test
    public void testHashCode() {
        assertEquals(SUT.hashCode(), new SnakOnlyStatementMerger(new StrictValueMatcher()).hashCode());
        assertNotEquals(SUT.hashCode(), new PropertyOnlyStatementMerger().hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(SUT.toString(), "SnakOnlyStatementMerger [valueMatcher=StrictValueMatcher]");
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        String json = "{\"type\":\"snak\",\"valueMatcher\":{\"type\":\"strict\"}}";
        TestUtils.isSerializedTo(SUT, json);
        assertEquals(ParsingUtilities.mapper.readValue(json, StatementMerger.class), SUT);
    }
}
