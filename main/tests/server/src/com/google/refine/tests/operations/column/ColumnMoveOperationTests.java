package com.google.refine.tests.operations.column;

import static org.mockito.Mockito.mock;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnMoveOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ColumnMoveOperationTests extends RefineTest {
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-move", ColumnMoveOperation.class);
    }
    
    @Test
    public void serializeColumnMoveOperation() throws JSONException, Exception {
        String json = "{\"op\":\"core/column-move\","
                + "\"description\":\"Move column my column to position 3\","
                + "\"columnName\":\"my column\","
                + "\"index\":3}";
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ColumnMoveOperation.reconstruct(project , new JSONObject(json)), json);
    }
}
