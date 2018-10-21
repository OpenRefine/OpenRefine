package com.google.refine.tests.model.recon;

import java.util.ArrayList;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class StandardReconConfigTests extends RefineTest {
    
    @BeforeMethod
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon", ReconOperation.class);
        ReconConfig.registerReconConfig(getCoreModule(), "standard-service", StandardReconConfig.class);
    }
    

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private class StandardReconConfigStub extends StandardReconConfig {
        public StandardReconConfigStub() {
            super("", "", "", "", "", false, new ArrayList<ColumnDetail>());
        }

        public double wordDistanceTest(String s1, String s2) {
            return wordDistance(s1, s2);
        }
    }

    @Test
    public void wordDistance() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("Foo", "Foo bar");

        Assert.assertEquals(0.5, r);
    }

    @Test
    public void wordDistanceOnlyStopwords() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("On and On", "On and On and On");

        Assert.assertTrue(!Double.isInfinite(r));
        Assert.assertTrue(!Double.isNaN(r));
    }
    
    @Test
    public void serializeStandardReconConfig() throws Exception {
        String json = " {\n" + 
                "        \"mode\": \"standard-service\",\n" + 
                "        \"service\": \"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\n" + 
                "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" + 
                "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" + 
                "        \"type\": {\n" + 
                "                \"id\": \"Q13442814\",\n" + 
                "                \"name\": \"scientific article\"\n" + 
                "        },\n" + 
                "        \"autoMatch\": true,\n" + 
                "        \"columnDetails\": [\n" + 
                "           {\n" + 
                "             \"column\": \"organization_country\",\n" + 
                "             \"propertyName\": \"SPARQL: P17/P297\",\n" + 
                "             \"propertyID\": \"P17/P297\"\n" + 
                "           },\n" + 
                "           {\n" + 
                "             \"column\": \"organization_id\",\n" + 
                "             \"propertyName\": \"SPARQL: P3500|P2427\",\n" + 
                "             \"propertyID\": \"P3500|P2427\"\n" + 
                "           }\n" + 
                "        ],\n" + 
                "        \"limit\": 0\n" + 
                " }";
        ReconConfig config = ReconConfig.reconstruct(json);
        TestUtils.isSerializedTo(config, json);
    }
}
