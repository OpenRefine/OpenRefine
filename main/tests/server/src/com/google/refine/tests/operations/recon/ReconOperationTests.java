package com.google.refine.tests.operations.recon;

import static org.mockito.Mockito.mock;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class ReconOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon", ReconOperation.class);
        ReconConfig.registerReconConfig(getCoreModule(), "standard-service", StandardReconConfig.class);
    }
    
    @Test
    public void serializeReconOperation() throws JSONException, Exception {
        String json = "{"
                + "\"op\":\"core/recon\","
                + "\"description\":\"Reconcile cells in column researcher to type Q5\","
                + "\"columnName\":\"researcher\","
                + "\"config\":{"
                + "   \"mode\":\"standard-service\","
                + "   \"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "   \"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "   \"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "   \"type\":{\"id\":\"Q5\",\"name\":\"human\"},"
                + "   \"autoMatch\":true,"
                + "   \"columnDetails\":[],"
                + "   \"limit\":0"
                + "},"
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
        Project project = mock(Project.class);
        TestUtils.isSerializedTo(ReconOperation.reconstruct(project, new JSONObject(json)), json);
    }
}
