package com.google.refine.tests.browsing.facets;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.tests.util.TestUtils;

public class EngineConfigTests {
    
    public static String engineConfigJson =
              "{\n"
            + "      \"mode\": \"row-based\",\n"
            + "      \"facets\": [\n"
            + "        {\n"
            + "          \"mode\": \"text\",\n"
            + "          \"invert\": false,\n"
            + "          \"caseSensitive\": false,\n"
            + "          \"query\": \"www\",\n"
            + "          \"name\": \"reference\",\n"
            + "          \"type\": \"text\",\n"
            + "          \"columnName\": \"reference\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }";
    
    public static String engineConfigRecordModeJson =
             "{"
            + "    \"mode\":\"record-based\","
            + "    \"facets\":[]"
            + "}";
    
    public static String noFacetProvided = "{\"mode\":\"row-based\"}";
    
    @Test
    public void serializeEngineConfig() {
        EngineConfig ec = EngineConfig.reconstruct(new JSONObject(engineConfigJson));
        TestUtils.isSerializedTo(ec, engineConfigJson);
    }
    
    @Test
    public void serializeEngineConfigRecordMode() {
        EngineConfig ec = EngineConfig.reconstruct(new JSONObject(engineConfigRecordModeJson));
        TestUtils.isSerializedTo(ec, engineConfigRecordModeJson);
    }
    
    @Test
    public void reconstructNullEngineConfig() {
        EngineConfig ec = EngineConfig.reconstruct(null);
        Assert.assertEquals(ec.getMode(), Mode.RowBased);
        Assert.assertTrue(ec.getFacetConfigs().isEmpty());
    }
    
    @Test
    public void reconstructNoFacetsProvided() {
        EngineConfig ec = EngineConfig.reconstruct(new JSONObject(noFacetProvided));
        Assert.assertEquals(ec.getMode(), Mode.RowBased);
        Assert.assertTrue(ec.getFacetConfigs().isEmpty());
    }
}
