package com.google.refine.tests.operations.row;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.row.RowRemovalOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class RowRemovalOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-removal", RowRemovalOperation.class);
    }
    
    @Test
    public void serializeRowRemovalOperation() throws JSONException, IOException {
        Project project = mock(Project.class);
        String json = "{"
                + "\"op\":\"core/row-removal\","
                + "\"description\":\"Remove rows\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
        TestUtils.isSerializedTo(RowRemovalOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
