package com.google.refine.tests.model;

import org.json.JSONException;
import org.testng.annotations.Test;

import com.google.refine.model.ReconType;
import com.google.refine.tests.util.TestUtils;

public class ReconTypeTest {
    @Test
    public void serializeReconType() throws JSONException, Exception {
        String json = "{\"id\":\"Q7540126\",\"name\":\"headquarters\"}";
        ReconType rt = ReconType.load(json);
        TestUtils.isSerializedTo(rt, json);
    }
}
