package com.google.refine.tests.operations.column;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnRenameOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class ColumnRenameOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-rename", ColumnRenameOperation.class);
    }
    
    @Test
    public void serializeColumnRenameOperation() throws JSONException, Exception {
        String json = "{\"op\":\"core/column-rename\","
                + "\"description\":\"Rename column old name to new name\","
                + "\"oldColumnName\":\"old name\","
                + "\"newColumnName\":\"new name\"}";
        JSONObject obj = new JSONObject(json);
        TestUtils.isSerializedTo(new ColumnRenameOperation(
            obj.getString("oldColumnName"),
            obj.getString("newColumnName")
        ), json);
    }
}
