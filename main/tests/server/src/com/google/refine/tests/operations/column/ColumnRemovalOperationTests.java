package com.google.refine.tests.operations.column;

import static org.mockito.Mockito.mock;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnRemovalOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class ColumnRemovalOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-removal", ColumnRemovalOperation.class);
    }
    
    @Test
    public void serializeColumnRemovalOperation() throws JSONException, Exception {
        String json = "{\"op\":\"core/column-removal\","
                + "\"description\":\"Remove column my column\","
                + "\"columnName\":\"my column\"}";
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ColumnRemovalOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
