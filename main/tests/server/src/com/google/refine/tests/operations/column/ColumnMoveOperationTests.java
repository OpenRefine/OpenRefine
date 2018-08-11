package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.AbstractOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.BlankDownOperation;
import com.google.refine.operations.column.ColumnMoveOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ColumnMoveOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-move", ColumnMoveOperation.class);
    }
    
    @Test
    public void serializeColumnMoveOperation() {
        AbstractOperation op = new ColumnMoveOperation("my column", 3);
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-move\","
                + "\"description\":\"Move column my column to position 3\","
                + "\"columnName\":\"my column\","
                + "\"index\":3}");
    }
}
