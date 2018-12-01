package com.google.refine.tests.clustering;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.browsing.Engine;
import com.google.refine.clustering.binning.BinningClusterer;
import com.google.refine.clustering.binning.BinningClusterer.BinningClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

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
    public void testSerializeBinningClustererConfig() throws JsonParseException, JsonMappingException, IOException {
        BinningClustererConfig config = ParsingUtilities.mapper.readValue(configJson, BinningClustererConfig.class);
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void testSerializeBinningClustererConfigWithNgrams() throws JsonParseException, JsonMappingException, IOException {
        BinningClustererConfig config = ParsingUtilities.mapper.readValue(configNgramJson, BinningClustererConfig.class);
        TestUtils.isSerializedTo(config, configNgramJson);
    }

    @Test
    public void testSerializeBinningClusterer() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("column\n"
                + "a\n"
                + "à\n"
                + "c\n"
                + "ĉ\n");
        BinningClustererConfig config = ParsingUtilities.mapper.readValue(configJson, BinningClustererConfig.class);
        BinningClusterer clusterer = config.apply(project);
        clusterer.computeClusters(new Engine(project));
        TestUtils.isSerializedTo(clusterer, clustererJson);
    }
}
