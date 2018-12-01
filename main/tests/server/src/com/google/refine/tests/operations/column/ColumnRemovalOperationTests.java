package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnRemovalOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;


public class ColumnRemovalOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-removal", ColumnRemovalOperation.class);
    }
    
    @Test
    public void serializeColumnRemovalOperation() throws Exception {
        String json = "{\"op\":\"core/column-removal\","
                + "\"description\":\"Remove column my column\","
                + "\"columnName\":\"my column\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnRemovalOperation.class), json);
    }
}
