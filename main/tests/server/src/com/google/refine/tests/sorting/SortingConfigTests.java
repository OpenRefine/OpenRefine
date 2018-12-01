package com.google.refine.tests.sorting;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.refine.sorting.SortingConfig;
import com.google.refine.tests.util.TestUtils;

public class SortingConfigTests {
    @Test
    public void serializeSortingConfig() throws IOException {
        String json = "{\n" + 
                "      \"criteria\": [\n" + 
                "        {\n" + 
                "          \"errorPosition\": 1,\n" + 
                "          \"valueType\": \"number\",\n" + 
                "          \"column\": \"start_year\",\n" + 
                "          \"blankPosition\": 2,\n" + 
                "          \"reverse\": false\n" + 
                "        }\n" + 
                "      ]\n" + 
                "    }";
        TestUtils.isSerializedTo(SortingConfig.reconstruct(json), json);
    }
}
