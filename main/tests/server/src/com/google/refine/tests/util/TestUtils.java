package com.google.refine.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.refine.Jsonizable;
import com.google.refine.util.ParsingUtilities;


public class TestUtils {
    
    static ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a temporary directory. NOTE: This is a quick and dirty
     * implementation suitable for tests, not production code.
     * 
     * @param name
     * @return
     * @throws IOException
     */
    public static File createTempDirectory(String name)
            throws IOException {
        File dir = File.createTempFile(name, "");
        dir.delete();
        dir.mkdir();
        return dir;
    }
    
    /**
     * Compare two JSON strings for equality.
     */
    public static void equalAsJson(String expected, String actual) {
        try {
            JsonNode jsonA = mapper.readValue(expected, JsonNode.class);
            JsonNode jsonB = mapper.readValue(actual, JsonNode.class);
            assertEquals(jsonA, jsonB);
        } catch(JsonMappingException e) {
            fail(e.getMessage());
        } catch (JsonParseException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Checks that a serializable object is serialized to the target JSON string.
     */
    public static void isSerializedTo(Jsonizable o, String targetJson, Properties options) {
        Writer w = new StringWriter();
        JSONWriter jsonWriter = new JSONWriter(w);
        o.write(jsonWriter, options);
        equalAsJson(targetJson, w.toString());
    }
    
    /**
     * Checks that a serializable object is serialized to the target JSON string.
     */
    public static void isSerializedTo(Jsonizable o, String targetJson) {
        isSerializedTo(o, targetJson, new Properties());
    }
}
