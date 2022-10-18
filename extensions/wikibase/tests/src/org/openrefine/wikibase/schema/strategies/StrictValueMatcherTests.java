
package org.openrefine.wikibase.schema.strategies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class StrictValueMatcherTests {

    StrictValueMatcher SUT = new StrictValueMatcher();

    @Test
    public void testMatching() {
        assertTrue(SUT.match(TestingData.existingId, TestingData.existingId));
        assertFalse(SUT.match(TestingData.existingId, TestingData.newIdA));
    }

    @Test
    public void testEquality() {
        assertEquals(SUT, new StrictValueMatcher());
        assertNotEquals(SUT, new LaxValueMatcher());
    }

    @Test
    public void testHashCode() {
        assertEquals(SUT.hashCode(), new StrictValueMatcher().hashCode());
        assertNotEquals(SUT.hashCode(), new LaxValueMatcher().hashCode());
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        TestUtils.assertEqualsAsJson(ParsingUtilities.mapper.writeValueAsString(SUT), "{\"type\":\"strict\"}");
    }
}
