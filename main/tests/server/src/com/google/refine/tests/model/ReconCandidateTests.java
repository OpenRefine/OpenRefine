package com.google.refine.tests.model;

import org.testng.annotations.Test;

import com.google.refine.model.ReconCandidate;
import com.google.refine.tests.util.TestUtils;

public class ReconCandidateTests {
    @Test
    public void serializeReconCandidateInt() throws Exception {
        String json = "{\"id\":\"Q49213\","
                + "\"name\":\"University of Texas at Austin\","
                + "\"score\":100,"
                + "\"types\":[\"Q875538\",\"Q15936437\",\"Q20971972\",\"Q23002039\"]}";
        ReconCandidate rc = ReconCandidate.loadStreaming(json);
        TestUtils.isSerializedTo(rc, json);
    }
    
    @Test
    public void serializeReconCandidateDouble() throws Exception {
        String json = "{\"id\":\"Q49213\","
                + "\"name\":\"University of Texas at Austin\","
                + "\"score\":0.5,"
                + "\"types\":[\"Q875538\",\"Q15936437\",\"Q20971972\",\"Q23002039\"]}";
        ReconCandidate rc = ReconCandidate.loadStreaming(json);
        TestUtils.isSerializedTo(rc, json);
    }
}
