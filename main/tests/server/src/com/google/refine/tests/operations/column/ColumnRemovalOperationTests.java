package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.AbstractOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnMoveOperation;
import com.google.refine.operations.column.ColumnRemovalOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class ColumnRemovalOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-removal", ColumnRemovalOperation.class);
    }
    
    @Test
    public void serializeColumnRemovalOperation() {
        AbstractOperation op = new ColumnRemovalOperation("my column");
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-removal\","
                + "\"description\":\"Remove column my column\","
                + "\"columnName\":\"my column\"}");
    }
}
