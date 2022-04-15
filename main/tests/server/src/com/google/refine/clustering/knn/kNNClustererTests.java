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

package com.google.refine.clustering.knn;

import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.clustering.knn.kNNClusterer;
import com.google.refine.clustering.knn.kNNClusterer.kNNClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

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

    @Test
    public void testNoLonelyclusters() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("column\n"
                + "foo\n"
                + "bar\n");
        kNNClustererConfig config = ParsingUtilities.mapper.readValue(configJson, kNNClustererConfig.class);
        kNNClusterer clusterer = config.apply(project);
        clusterer.computeClusters(new Engine(project));

        assertTrue(clusterer.getJsonRepresentation().isEmpty());
    }
}
