
package org.openrefine.model;

import static org.testng.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.annotations.Test;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnInsertionTests {

    @Test
    public void testSerializeFull() throws JsonMappingException, JsonProcessingException {
        String json = "{\n"
                + "  \"copiedFrom\" : \"original\","
                + "  \"insertAt\" : \"bar\","
                + "  \"replace\" : true,"
                + "  \"name\" : \"foo\""
                + "}";

        ColumnInsertion SUT = new ColumnInsertion("foo", "bar", true, "original", null);

        TestUtils.isSerializedTo(SUT, json, ParsingUtilities.defaultWriter);
        assertEquals(ParsingUtilities.mapper.readValue(json, ColumnInsertion.class), SUT);
    }

    @Test
    public void testSerializeMinimal() throws JsonMappingException, JsonProcessingException {
        String json = "{\"name\":\"foo\", \"replace\": false}";

        ColumnInsertion SUT = new ColumnInsertion("foo", null, false, null, null);

        TestUtils.isSerializedTo(SUT, json, ParsingUtilities.defaultWriter);
        assertEquals(ParsingUtilities.mapper.readValue(json, ColumnInsertion.class), SUT);
    }

}
