package com.google.refine.tests.sorting;

import java.io.IOException;

import org.json.JSONException;
import org.testng.annotations.Test;

import com.google.refine.sorting.Criterion;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class BooleanCriterionTest {
    @Test
    public void serializeBooleanCriterion() throws JSONException, IOException {
        String json = 
                "        {\n" + 
                "          \"errorPosition\": 1,\n" + 
                "          \"valueType\": \"boolean\",\n" + 
                "          \"column\": \"start_year\",\n" + 
                "          \"blankPosition\": 2,\n" + 
                "          \"reverse\": false\n" + 
                "        }\n";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, Criterion.class), json);
    }
}
