package com.google.refine.tests.model;

import java.util.ArrayList;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.tests.RefineTest;

public class ReconTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private class StandardReconConfigTest extends StandardReconConfig {
        public StandardReconConfigTest() {
            super("", "", "", "", "", false, new ArrayList<ColumnDetail>());
        }

        public double wordDistanceTest(String s1, String s2) {
            return wordDistance(s1, s2);
        }
    }

    @Test
    public void wordDistance() {
        StandardReconConfigTest t = new StandardReconConfigTest();
        double r = t.wordDistanceTest("Foo", "Foo bar");

        Assert.assertEquals(r,0.5);
    }

    @Test
    public void wordDistanceOnlyStopwords() {
        StandardReconConfigTest t = new StandardReconConfigTest();
        double r = t.wordDistanceTest("On and On", "On and On and On");

        Assert.assertTrue(!Double.isInfinite(r));
        Assert.assertTrue(!Double.isNaN(r));
    }
    
    /**
     * Regression for issue #1517:
     * JSON deserialization exception due to the upgrade of org.json library in data package PR
     * @throws Exception
     */
    @Test
    public void limitJSONKeyTest() throws Exception {
        JSONObject obj = new JSONObject(
                " {\n" + 
                "        \"mode\": \"standard-service\",\n" + 
                "        \"service\": \"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\n" + 
                "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" + 
                "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" + 
                "        \"type\": {\n" + 
                "                \"id\": \"Q13442814\",\n" + 
                "                \"name\": \"scientific article\"\n" + 
                "        },\n" + 
                "        \"autoMatch\": true,\n" + 
                "        \"columnDetails\": [],\n" + 
                "        \"limit\": 0\n" + 
                " }");
        
        ReconConfig config = StandardReconConfig.reconstruct(obj);
        
        // Assert the object is created
        Assert.assertTrue(config != null);
    }
}
