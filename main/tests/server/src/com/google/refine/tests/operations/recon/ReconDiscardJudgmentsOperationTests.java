package com.google.refine.tests.operations.recon;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconDiscardJudgmentsOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ReconDiscardJudgmentsOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-discard-judgments", ReconDiscardJudgmentsOperation.class);
    }
    
    @Test
    public void serializeReconDiscardJudgmentsOperation() throws Exception {
        String json = "{\n" + 
                "    \"op\": \"core/recon-discard-judgments\",\n" + 
                "    \"description\": \"Discard recon judgments and clear recon data for cells in column researcher\",\n" + 
                "    \"engineConfig\": {\n" + 
                "      \"mode\": \"record-based\",\n" + 
                "      \"facets\": []\n" + 
                "    },\n" + 
                "    \"columnName\": \"researcher\",\n" + 
                "    \"clearData\": true\n" + 
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconDiscardJudgmentsOperation.class), json);
    }
}
