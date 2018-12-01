package com.google.refine.tests.clustering;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.browsing.Engine;
import com.google.refine.clustering.knn.kNNClusterer;
import com.google.refine.clustering.knn.kNNClusterer.kNNClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

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
    public void serializekNNClustererConfig() throws JsonParseException, JsonMappingException, IOException {
        kNNClustererConfig config = ParsingUtilities.mapper.readValue(configJson, kNNClustererConfig.class);
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void serializekNNClusterer() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("column\n"
                + "ab\n"
                + "abc\n"
                + "c\n"
                + "ĉ\n");
        
        kNNClustererConfig config = ParsingUtilities.mapper.readValue(configJson, kNNClustererConfig.class);
        kNNClusterer clusterer = config.apply(project);
        clusterer.computeClusters(new Engine(project));
        
        TestUtils.isSerializedTo(clusterer, clustererJson);
    }
}
