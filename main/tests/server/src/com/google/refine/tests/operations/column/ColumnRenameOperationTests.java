package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.AbstractOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnMoveOperation;
import com.google.refine.operations.column.ColumnRenameOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class ColumnRenameOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-rename", ColumnRenameOperation.class);
    }
    
    @Test
    public void serializeColumnRenameOperation() {
        AbstractOperation op = new ColumnRenameOperation("old name", "new name");
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-rename\","
                + "\"description\":\"Rename column old name to new name\","
                + "\"oldColumnName\":\"old name\","
                + "\"newColumnName\":\"new name\"}");
    }
}
