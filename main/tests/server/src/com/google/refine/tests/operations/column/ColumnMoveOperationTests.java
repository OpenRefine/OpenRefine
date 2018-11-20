package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnMoveOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ColumnMoveOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-move", ColumnMoveOperation.class);
    }
    
    @Test
    public void serializeColumnMoveOperation() throws Exception {
        String json = "{\"op\":\"core/column-move\","
                + "\"description\":\"Move column my column to position 3\","
                + "\"columnName\":\"my column\","
                + "\"index\":3}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnMoveOperation.class), json);
    }
}
