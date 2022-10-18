
package org.openrefine.wikibase.schema.strategies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class PropertyOnlyStatementMergerTests {

    PropertyOnlyStatementMerger SUT = new PropertyOnlyStatementMerger();

    PropertyIdValue pid1 = TestingData.existingPropertyId;
    PropertyIdValue pid2 = TestingData.newPropertyIdA;
    Value value1 = Datamodel.makeWikidataItemIdValue("Q4894");
    Value value2 = Datamodel.makeWikidataItemIdValue("Q3645");

    Statement statement1 = TestingData.generateStatement(
            TestingData.matchedId,
            pid1,
            value1);
    Statement statement2 = TestingData.generateStatement(
            TestingData.matchedId,
            pid1,
            value2);
    Statement statement3 = TestingData.generateStatement(
            TestingData.matchedId,
            pid2,
            value2);

    @Test
    public void testMatch() {
        assertTrue(SUT.match(statement1, statement1));
        assertTrue(SUT.match(statement1, statement2));
        assertFalse(SUT.match(statement1, statement3));
    }

    @Test
    public void testMerge() {
        assertEquals(SUT.merge(statement1, statement1), statement1);
        assertEquals(SUT.merge(statement1, statement2), statement1);
    }

    @Test
    public void testEquals() {
        assertEquals(SUT, new PropertyOnlyStatementMerger());
        assertNotEquals(SUT, new SnakOnlyStatementMerger(new StrictValueMatcher()));
    }

    @Test
    public void testHashCode() {
        assertEquals(SUT.hashCode(), new PropertyOnlyStatementMerger().hashCode());
        assertNotEquals(SUT.hashCode(), new SnakOnlyStatementMerger(new StrictValueMatcher()).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(SUT.toString(), "PropertyOnlyStatementMerger");
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        TestUtils.assertEqualsAsJson(ParsingUtilities.mapper.writeValueAsString(SUT), "{\"type\":\"property\"}");
    }
}
