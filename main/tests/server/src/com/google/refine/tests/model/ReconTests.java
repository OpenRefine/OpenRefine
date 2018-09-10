package com.google.refine.tests.model;

import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.model.Recon;
import com.google.refine.tests.util.TestUtils;

public class ReconTests {
    @Test
    public void serializeRecon() throws Exception {
        Properties options = new Properties();
        options.put("mode", "save");
        
        String fullJson = "{\"id\":1533651559492945033,"
                + "\"judgmentHistoryEntry\":1533651616890,"
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "\"j\":\"matched\","
                + "\"m\":{"
                + "   \"id\":\"Q2892284\","
                + "   \"name\":\"Baylor College of Medicine\","
                + "   \"score\":98.57142857142858,"
                + "   \"types\":[\"Q16917\",\"Q23002054\",\"Q494230\"]"
                + "},"
                + "\"c\":["
                + "   {\"id\":\"Q2892284\",\"name\":\"Baylor College of Medicine\",\"score\":98.57142857142858,\"types\":[\"Q16917\",\"Q23002054\",\"Q494230\"]},"
                + "   {\"id\":\"Q16165943\",\"name\":\"Baylor College of Medicine Academy at Ryan\",\"score\":82.14285714285715,\"types\":[\"Q149566\"]},"
                + "   {\"id\":\"Q30284245\",\"name\":\"Baylor College of Medicine Children\\u2019s Foundation\",\"score\":48.57142857142858,\"types\":[\"Q163740\"]}"
                + "],"
                + "\"f\":[false,false,1,0.6666666666666666],"
                + "\"judgmentAction\":\"mass\","
                + "\"judgmentBatchSize\":1,"
                + "\"matchRank\":0}";
        Recon r = Recon.loadStreaming(fullJson, null);
        TestUtils.isSerializedTo(r, fullJson, options);
        
        String shortJson = "{\"id\":1533651559492945033,"
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "\"j\":\"matched\","
                + "\"m\":{"
                + "   \"id\":\"Q2892284\","
                + "   \"name\":\"Baylor College of Medicine\","
                + "   \"score\":98.57142857142858,"
                + "   \"types\":[\"Q16917\",\"Q23002054\",\"Q494230\"]"
                + "}}";
        options.put("mode", "normal");
        TestUtils.isSerializedTo(r, shortJson, options);
    }
    

}
