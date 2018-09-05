package com.google.refine.tests.clustering;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.clustering.knn.kNNClusterer;
import com.google.refine.clustering.knn.kNNClusterer.kNNClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class kNNClustererTests extends RefineTest {
    
    public static String configJson = "{"
            + "\"type\":\"knn\","
            + "\"function\":\"PPM\","
            + "\"column\":\"values\","
            + "\"params\":{\"radius\":1,\"blocking-ngram-size\":2}"
            + "}";
    public static String clustererJson = "["
            + "   [{\"v\":\"ab\",\"c\":1},{\"v\":\"abc\",\"c\":1}]"
            + "]";
    
    @Test
    public void serializekNNClustererConfig() {
        kNNClustererConfig config = new kNNClustererConfig();
        config.initializeFromJSON(new JSONObject(configJson));
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void serializekNNClusterer() {
        Project project = createCSVProject("column\n"
                + "ab\n"
                + "abc\n"
                + "c\n"
                + "ĉ\n");
        
        kNNClustererConfig config = new kNNClustererConfig();
        config.initializeFromJSON(new JSONObject(configJson));
        kNNClusterer clusterer = config.apply(project);
        clusterer.computeClusters(new Engine(project));
        
        TestUtils.isSerializedTo(clusterer, clustererJson);
    }
}
