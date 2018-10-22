package com.google.refine.tests.operations.row;

import static org.mockito.Mockito.mock;

import org.json.JSONException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.row.DenormalizeOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class DenormalizeOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "denormalize", DenormalizeOperation.class);
    }
    
    @Test
    public void serializeDenormalizeOperation() throws JSONException, Exception {
        Project project = mock(Project.class);
        String json = "{"
                + "\"op\":\"core/denormalize\","
                + "\"description\":\"Denormalize\"}";
        TestUtils.isSerializedTo(new DenormalizeOperation(), json);
    }
}
