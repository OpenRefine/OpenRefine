package com.google.refine.tests.operations.column;

import static org.mockito.Mockito.mock;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
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
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ColumnRenameOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
