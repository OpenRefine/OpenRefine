package com.google.refine.tests.browsing.facets;

import org.json.JSONObject;
import org.testng.annotations.Test;

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
    
    @Test
    public void serializeEngineConfig() {
        EngineConfig ec = EngineConfig.reconstruct(new JSONObject(engineConfigJson));
        TestUtils.isSerializedTo(ec, engineConfigJson);
    }
}
