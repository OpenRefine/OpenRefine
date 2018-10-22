package com.google.refine.tests.sorting;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.refine.sorting.Criterion;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class DateCriterionTest {
    @Test
    public void serializeDateCriterion() throws IOException {
        String json = 
                "        {\n" + 
                "          \"errorPosition\": 2,\n" + 
                "          \"valueType\": \"date\",\n" + 
                "          \"column\": \"start_year\",\n" + 
                "          \"blankPosition\": -1,\n" + 
                "          \"reverse\": true\n" + 
                "        }\n";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, Criterion.class), json);
    }
}
