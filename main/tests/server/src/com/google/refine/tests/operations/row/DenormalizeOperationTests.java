package com.google.refine.tests.operations.row;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.row.DenormalizeOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class DenormalizeOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "denormalize", DenormalizeOperation.class);
    }
    
    @Test
    public void serializeDenormalizeOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/denormalize\","
                + "\"description\":\"Denormalize\"}";
        TestUtils.isSerializedTo(new DenormalizeOperation(), json);
    }
}
