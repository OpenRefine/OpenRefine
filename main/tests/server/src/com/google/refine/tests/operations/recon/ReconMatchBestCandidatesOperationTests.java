package com.google.refine.tests.operations.recon;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconMatchBestCandidatesOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ReconMatchBestCandidatesOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-match-best-candidates", ReconMatchBestCandidatesOperation.class);
    }
    
    @Test
    public void serializeReconMatchBestCandidatesOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/recon-match-best-candidates\","
                + "\"description\":\"Match each cell to its best recon candidate in column organization_name\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":["
                + "       {\"selectNumeric\":true,\"expression\":\"cell.recon.best.score\",\"selectBlank\":false,\"selectNonNumeric\":true,\"selectError\":true,\"name\":\"organization_name: best candidate's score\",\"from\":13,\"to\":101,\"type\":\"range\",\"columnName\":\"organization_name\"},"
                + "       {\"selectNonTime\":true,\"expression\":\"grel:toDate(value)\",\"selectBlank\":true,\"selectError\":true,\"selectTime\":true,\"name\":\"start_year\",\"from\":410242968000,\"to\":1262309184000,\"type\":\"timerange\",\"columnName\":\"start_year\"}"
                + "]},"
                + "\"columnName\":\"organization_name\""
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconMatchBestCandidatesOperation.class), json);
    }
}
