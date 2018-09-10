package com.google.refine.tests.operations.column;

import static org.mockito.Mockito.mock;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnAdditionOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ColumnAdditionOperationTests extends RefineTest {
    
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "column-addition", ColumnAdditionOperation.class);
    }
    
    @Test
    public void serializeColumnAdditionOperation() throws JSONException, Exception {
        String json = "{"
                + "   \"op\":\"core/column-addition\","
                + "   \"description\":\"Create column organization_json at index 3 based on column employments using expression grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\",\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},\"newColumnName\":\"organization_json\",\"columnInsertIndex\":3,\"baseColumnName\":\"employments\","
                + "    \"expression\":\"grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\","
                + "   \"onError\":\"set-to-blank\""
                + "}";
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ColumnAdditionOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
