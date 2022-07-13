/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.clustering.binning;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.clustering.binning.BinningClusterer.BinningClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

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

    @Test
    public void testNoLonelyClusters() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("column\n"
                + "c\n"
                + "ĉ\n"
                + "d\n");
        BinningClustererConfig config = ParsingUtilities.mapper.readValue(configJson, BinningClustererConfig.class);
        BinningClusterer clusterer = config.apply(project);
        clusterer.computeClusters(new Engine(project));
        assertEquals(clusterer.getJsonRepresentation().size(), 1);
    }
}
