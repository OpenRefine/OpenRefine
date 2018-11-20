package com.google.refine.tests.operations.column;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.AbstractOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnRenameOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;


public class ColumnRenameOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-rename", ColumnRenameOperation.class);
    }
    
    @Test
    public void serializeColumnRenameOperation() throws Exception {
        String json = "{\"op\":\"core/column-rename\","
                + "\"description\":\"Rename column old name to new name\","
                + "\"oldColumnName\":\"old name\","
                + "\"newColumnName\":\"new name\"}";
        AbstractOperation op = ParsingUtilities.mapper.readValue(json, AbstractOperation.class);
        TestUtils.isSerializedTo(op, json);
    }
}
