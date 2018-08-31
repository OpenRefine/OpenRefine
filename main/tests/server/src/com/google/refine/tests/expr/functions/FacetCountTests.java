package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.FacetCount;
import com.google.refine.tests.util.TestUtils;

public class FacetCountTests {
    @Test
    public void serializeFacetCount() {
        String json = "{\"description\":\"Returns the facet count corresponding to the given choice value\",\"params\":\"choiceValue, string facetExpression, string columnName\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new FacetCount(), json);
    }
}

