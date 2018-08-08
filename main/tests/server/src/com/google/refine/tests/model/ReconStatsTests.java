package com.google.refine.tests.model;

import org.testng.annotations.Test;

import com.google.refine.model.ReconStats;
import com.google.refine.tests.util.TestUtils;

public class ReconStatsTests {
    
    @Test
    public void serializeReconStats() {
        ReconStats rs = new ReconStats(3, 1, 2);
        TestUtils.isSerializedTo(rs,"{\"nonBlanks\":3,\"newTopics\":1,\"matchedTopics\":2}");
    }
}
