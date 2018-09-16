package com.google.refine.tests.clustering;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.clustering.binning.BinningClusterer;
import com.google.refine.clustering.binning.BinningClusterer.BinningClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class BinningClustererTests extends RefineTest {
    
    String configJson = "{"
            + "\"type\":\"binning\","
            + "\"function\":\"fingerprint\","
            + "\"column\":\"values\","
            + "\"params\":{}}";
    
    String configNgramJson = "{"
            + "\"type\":\"binning\","
            + "\"function\":\"ngram-fingerprint\","
            + "\"column\":\"values\","
            + "\"params\":{\"ngram-size\":2}}";
    
    String clustererJson = "["
            + "  [{\"v\":\"a\",\"c\":1},{\"v\":\"à\",\"c\":1}],"
            + "  [{\"v\":\"c\",\"c\":1},{\"v\":\"ĉ\",\"c\":1}]"
            + "]";
    
    @Test
    public void testSerializeBinningClustererConfig() {
        BinningClustererConfig config = new BinningClustererConfig();
        config.initializeFromJSON(new JSONObject(configJson));
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void testSerializeBinningClustererConfigWithNgrams() {
        BinningClustererConfig config = new BinningClustererConfig();
        config.initializeFromJSON(new JSONObject(configNgramJson));
        TestUtils.isSerializedTo(config, configNgramJson);
    }

    @Test
    public void testSerializeBinningClusterer() {
        Project project = createCSVProject("column\n"
                + "a\n"
                + "à\n"
                + "c\n"
                + "ĉ\n");
        BinningClustererConfig config = new BinningClustererConfig();
        config.initializeFromJSON(new JSONObject(configJson));
        BinningClusterer clusterer = config.apply(project);
        clusterer.computeClusters(new Engine(project));
        TestUtils.isSerializedTo(clusterer, clustererJson);
    }
}
