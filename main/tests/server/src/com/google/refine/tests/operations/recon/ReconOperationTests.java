package com.google.refine.tests.operations.recon;

import static org.mockito.Mockito.mock;

import java.util.Properties;

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
    private String json= "{"
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
    private Project project = mock(Project.class);
    
    private String processJson = ""
            + "    {\n" + 
            "       \"description\" : \"Reconcile cells in column researcher to type Q5\",\n" + 
            "       \"id\" : %d,\n" + 
            "       \"immediate\" : false,\n" + 
            "       \"onDone\" : [ {\n" + 
            "         \"action\" : \"createFacet\",\n" + 
            "         \"facetConfig\" : {\n" + 
            "           \"columnName\" : \"researcher\",\n" + 
            "           \"expression\" : \"forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), \\\"(unreconciled)\\\", \\\"(blank)\\\"))\",\n" + 
            "           \"name\" : \"researcher: judgment\"\n" + 
            "         },\n" + 
            "         \"facetOptions\" : {\n" + 
            "           \"scroll\" : false\n" + 
            "         },\n" + 
            "         \"facetType\" : \"list\"\n" + 
            "       }, {\n" + 
            "         \"action\" : \"createFacet\",\n" + 
            "         \"facetConfig\" : {\n" + 
            "           \"columnName\" : \"researcher\",\n" + 
            "           \"expression\" : \"cell.recon.best.score\",\n" + 
            "           \"mode\" : \"range\",\n" + 
            "           \"name\" : \"researcher: best candidate's score\"\n" + 
            "         },\n" + 
            "         \"facetType\" : \"range\"\n" + 
            "       } ],\n" + 
            "       \"progress\" : 0,\n" + 
            "       \"status\" : \"pending\"\n" + 
            "     }";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon", ReconOperation.class);
        ReconConfig.registerReconConfig(getCoreModule(), "standard-service", StandardReconConfig.class);
    }
    
    @Test
    public void serializeReconOperation() throws JSONException, Exception {
        TestUtils.isSerializedTo(ReconOperation.reconstruct(project, new JSONObject(json)), json);
    }
    
    @Test
    public void serializeReconProcess() throws JSONException, Exception {
        ReconOperation op = ReconOperation.reconstruct(project, new JSONObject(json));
        com.google.refine.process.Process process = op.createProcess(project, new Properties());
        TestUtils.isSerializedTo(process, String.format(processJson, process.hashCode()));
    }
}
