package com.google.refine.tests.operations.recon;
import static org.mockito.Mockito.mock;

import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconClearSimilarCellsOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ReconClearSimilarCellsOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-clear-similar-cells", ReconClearSimilarCellsOperation.class);
    }
    
    @Test
    public void serializeReconClearSimilarCellsOperation() throws Exception {
        String json = "{\"op\":\"core/recon-clear-similar-cells\","
                + "\"description\":\"Clear recon data for cells containing \\\"some value\\\" in column my column\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\","
                + "\"similarValue\":\"some value\"}";
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ReconClearSimilarCellsOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
