
package org.openrefine.wikibase.schema.strategies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class LaxValueMatcherTests {

    LaxValueMatcher SUT = new LaxValueMatcher();

    @Test
    public void testQids() {
        EntityIdValue qid1 = Datamodel.makeItemIdValue("Q123", "https://foo.com");
        EntityIdValue qid2 = Datamodel.makeItemIdValue("Q123", "https://bar.com");
        EntityIdValue qid3 = Datamodel.makeItemIdValue("Q456", "https://foo.com");

        assertTrue(SUT.match(qid1, qid1));
        assertTrue(SUT.match(qid1, qid2));
        assertFalse(SUT.match(qid1, qid3));
    }

    @Test
    public void testUrls() {
        StringValue value1 = Datamodel.makeStringValue("https://gnu.org");
        StringValue value2 = Datamodel.makeStringValue("http://gnu.org/");
        StringValue value3 = Datamodel.makeStringValue("http://gnu.org/page");

        assertTrue(SUT.match(value1, value2));
        assertTrue(SUT.match(value1, value1));
        assertFalse(SUT.match(value2, value3));
    }

    @Test
    public void testWhitespace() {
        StringValue value1 = Datamodel.makeStringValue("foo");
        StringValue value2 = Datamodel.makeStringValue("\tfoo ");
        StringValue value3 = Datamodel.makeStringValue("bar");

        assertTrue(SUT.match(value1, value2));
        assertFalse(SUT.match(value1, value3));
    }

    @Test
    public void testMonolingualText() {
        MonolingualTextValue value1 = Datamodel.makeMonolingualTextValue("foo", "en");
        MonolingualTextValue value2 = Datamodel.makeMonolingualTextValue("\tfoo ", "en");
        MonolingualTextValue value3 = Datamodel.makeMonolingualTextValue("bar", "en");

        assertTrue(SUT.match(value1, value2));
        assertFalse(SUT.match(value1, value3));
    }

    @Test
    public void testQuantity() {
        QuantityValue value1 = Datamodel.makeQuantityValue(new BigDecimal("1.234"));
        QuantityValue value2 = Datamodel.makeQuantityValue(new BigDecimal("1.2"), new BigDecimal("1"), new BigDecimal("1.3"));
        QuantityValue value3 = Datamodel.makeQuantityValue(new BigDecimal("1.35"), new BigDecimal("1.25"), new BigDecimal("1.45"));
        QuantityValue value4 = Datamodel.makeQuantityValue(new BigDecimal("1.234"), Datamodel.makeWikidataItemIdValue("Q34"));
        QuantityValue value5 = Datamodel.makeQuantityValue(new BigDecimal("1.5"), new BigDecimal("1"), new BigDecimal("2"));

        assertTrue(SUT.match(value1, value1));
        assertTrue(SUT.match(value1, value2));
        assertTrue(SUT.match(value2, value1));
        assertFalse(SUT.match(value1, value3));
        assertFalse(SUT.match(value3, value1));
        assertFalse(SUT.match(value1, value4));
        assertFalse(SUT.match(value4, value1));
        assertTrue(SUT.match(value2, value2));
        assertTrue(SUT.match(value2, value3));
        assertTrue(SUT.match(value5, value3));
        assertTrue(SUT.match(value3, value5));
    }

    @Test
    public void testGlobeCoordinates() {
        GlobeCoordinatesValue value1 = Datamodel.makeGlobeCoordinatesValue(34.5, 42.5, .5, "https://foo.org/entity/Q123");
        GlobeCoordinatesValue value2 = Datamodel.makeGlobeCoordinatesValue(34.5, 42.5, .5, "https://foo.org/entity/Q456");
        GlobeCoordinatesValue value3 = Datamodel.makeGlobeCoordinatesValue(34.25, 42.25, 1, "https://foo.org/entity/Q123");
        GlobeCoordinatesValue value4 = Datamodel.makeGlobeCoordinatesValue(34.25, 62.25, 1, "https://foo.org/entity/Q123");

        assertTrue(SUT.match(value1, value1));
        assertFalse(SUT.match(value1, value2));
        assertFalse(SUT.match(value2, value1));
        assertTrue(SUT.match(value1, value3));
        assertTrue(SUT.match(value3, value1));
        assertFalse(SUT.match(value1, value4));
        assertFalse(SUT.match(value4, value1));
    }

    @Test
    public void testTimeValue() {
        String calModel = "https://foo.com/calendars/Q123";
        TimeValue day1 = Datamodel.makeTimeValue(1987, (byte) 3, (byte) 8, (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_DAY, 0, 0, 0,
                calModel);
        TimeValue day2 = Datamodel.makeTimeValue(1987, (byte) 11, (byte) 10, (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_DAY, 0, 0, 0,
                calModel);
        TimeValue day3 = Datamodel.makeTimeValue(1987, (byte) 3, (byte) 8, (byte) 10, (byte) 11, (byte) 12, TimeValue.PREC_DAY, 0, 0, 0,
                calModel);
        TimeValue year1 = Datamodel.makeTimeValue(1987, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_YEAR, 0, 0, 0,
                calModel);
        TimeValue year2 = Datamodel.makeTimeValue(1987, (byte) 5, (byte) 6, (byte) 7, (byte) 0, (byte) 0, TimeValue.PREC_YEAR, 0, 0, 0,
                calModel);

        assertTrue(SUT.match(day1, day1));
        assertFalse(SUT.match(day1, day2));
        assertFalse(SUT.match(day2, day1));
        assertTrue(SUT.match(day1, day3));
        assertTrue(SUT.match(day3, day1));
        assertTrue(SUT.match(year1, year2));
        assertTrue(SUT.match(year2, year1));
    }

    @Test
    public void testEquality() {
        assertEquals(SUT, new LaxValueMatcher());
        assertNotEquals(SUT, new StrictValueMatcher());
    }

    @Test
    public void testHashCode() {
        assertEquals(SUT.hashCode(), new LaxValueMatcher().hashCode());
        assertNotEquals(SUT.hashCode(), new StrictValueMatcher().hashCode());
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        TestUtils.assertEqualsAsJson(ParsingUtilities.mapper.writeValueAsString(SUT), "{\"type\":\"lax\"}");
    }

}
