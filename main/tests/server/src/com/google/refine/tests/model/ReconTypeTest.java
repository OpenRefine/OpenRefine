package com.google.refine.tests.model;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.model.ReconType;
import com.google.refine.tests.util.TestUtils;

public class ReconTypeTest {
    @Test
    public void serializeReconType() throws IOException {
        String json = "{\"id\":\"Q7540126\",\"name\":\"headquarters\"}";
        ReconType rt = ReconType.load(json);
        TestUtils.isSerializedTo(rt, json);
    }
    
    @Test
    public void deserializeFromString() throws IOException {
    	// reconciliation services can return lists of types as bare lists of strings
    	ReconType rt = ReconType.load("\"Q7540126\"");
    	Assert.assertEquals(rt.id, "Q7540126");
    }
}
