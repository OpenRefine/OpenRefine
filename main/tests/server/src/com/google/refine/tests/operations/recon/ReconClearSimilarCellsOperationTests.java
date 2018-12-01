package com.google.refine.tests.operations.recon;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconClearSimilarCellsOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

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
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconClearSimilarCellsOperation.class), json);
    }
}
