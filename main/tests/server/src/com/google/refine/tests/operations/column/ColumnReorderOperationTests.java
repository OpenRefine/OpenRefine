package com.google.refine.tests.operations.column;

import java.util.Arrays;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.AbstractOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnReorderOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class ColumnReorderOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-reorder", ColumnReorderOperation.class);
    }
    
    @Test
    public void serializeColumnReorderOperation() {
        AbstractOperation op = new ColumnReorderOperation(Arrays.asList("b","c","a"));
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-reorder\","
                + "\"description\":\"Reorder columns\","
                + "\"columnNames\":[\"b\",\"c\",\"a\"]}");
    }
}
