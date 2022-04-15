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

package com.google.refine.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.model.Recon.Judgment;
import com.google.refine.util.TestUtils;

public class ReconTests {

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

    @Test
    public void serializeReconSaveMode() throws Exception {
        Recon r = Recon.loadStreaming(fullJson);
        TestUtils.isSerializedTo(r, fullJson, true);
    }

    @Test
    public void serializeReconViewMode() throws Exception {
        Recon r = Recon.loadStreaming(fullJson);
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
        TestUtils.isSerializedTo(r, shortJson, false);
    }

    @Test
    public void serializeReconSaveModeNoMatch() throws Exception {
        String json = "{\"id\":1533651559492945033,"
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "\"j\":\"none\","
                + "\"c\":["
                + "   {\"id\":\"Q2892284\",\"name\":\"Baylor College of Medicine\",\"score\":98.57142857142858,\"types\":[\"Q16917\",\"Q23002054\",\"Q494230\"]},"
                + "   {\"id\":\"Q16165943\",\"name\":\"Baylor College of Medicine Academy at Ryan\",\"score\":82.14285714285715,\"types\":[\"Q149566\"]},"
                + "   {\"id\":\"Q30284245\",\"name\":\"Baylor College of Medicine Children\\u2019s Foundation\",\"score\":48.57142857142858,\"types\":[\"Q163740\"]}"
                + "]"
                + "}";
        Recon r = Recon.loadStreaming(fullJson);
        r.match = null;
        r.judgment = Judgment.None;
        TestUtils.isSerializedTo(r, json);
    }

    /**
     * Test for issue https://github.com/OpenRefine/OpenRefine/issues/3785. Generating many recon objects within a short
     * amount of time leads to collisions in id generation.
     */
    @Test
    public void randomIdGeneration() {
        long numberOfSamples = 100000L;
        String space = "http://some.url/";
        long judgmentHistoryId = 1234L;
        Set<Long> ids = LongStream.range(0L, numberOfSamples)
                .mapToObj(i -> new Recon(judgmentHistoryId, space, space).id)
                .collect(Collectors.toSet());
        // make sure we generated as many ids as Recon objects (if ids.size() is smaller,
        // then we have had some collisions)
        Assert.assertEquals(ids.size(), numberOfSamples);
    }

}
