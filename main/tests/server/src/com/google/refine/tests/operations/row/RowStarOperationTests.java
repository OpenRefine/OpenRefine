package com.google.refine.tests.operations.row;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.row.RowStarOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class RowStarOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-star", RowStarOperation.class);
    }
    
    @Test
    public void serializeRowStarOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/row-star\","
                + "\"description\":\"Star rows\","
                + "\"starred\":true,"
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, RowStarOperation.class), json);
    }
}
