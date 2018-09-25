package com.google.refine.tests.sorting;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.sorting.Criterion;
import com.google.refine.tests.util.TestUtils;

public class BooleanCriterionTest {
    @Test
    public void serializeBooleanCriterion() {
        String json = 
                "        {\n" + 
                "          \"errorPosition\": 1,\n" + 
                "          \"valueType\": \"boolean\",\n" + 
                "          \"column\": \"start_year\",\n" + 
                "          \"blankPosition\": 2,\n" + 
                "          \"reverse\": false\n" + 
                "        }\n";
        TestUtils.isSerializedTo(Criterion.reconstruct(new JSONObject(json)), json);
    }
}
