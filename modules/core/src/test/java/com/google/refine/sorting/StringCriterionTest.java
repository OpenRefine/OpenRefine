
package com.google.refine.sorting;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class StringCriterionTest {

    String json = "{\n" +
            "  \"errorPosition\": 2,\n" +
            "  \"valueType\": \"string\",\n" +
            "  \"column\": \"start_year\",\n" +
            "  \"blankPosition\": 1,\n" +
            "  \"reverse\": true,\n" +
            "  \"caseSensitive\": true\n" +
            "}\n";

    @Test
    public void serializeNumberCriterion() throws IOException {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, Criterion.class), json);
    }

    @Test
    public void testRenameColumn() throws Exception {
        String renamedJson = "{\n" +
                "  \"errorPosition\": 2,\n" +
                "  \"valueType\": \"string\",\n" +
                "  \"column\": \"start\",\n" +
                "  \"blankPosition\": 1,\n" +
                "  \"reverse\": true,\n" +
                "  \"caseSensitive\": true\n" +
                "}\n";
        Criterion renamed = ParsingUtilities.mapper.readValue(json, Criterion.class).renameColumns(Map.of("start_year", "start"));
        TestUtils.isSerializedTo(renamed, renamedJson);
    }
}
